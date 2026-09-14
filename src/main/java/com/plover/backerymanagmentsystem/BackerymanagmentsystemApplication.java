package com.plover.backerymanagmentsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import com.plover.backerymanagmentsystem.manager.repository.CustomerRepository;
import com.plover.backerymanagmentsystem.manager.repository.CustomerPointsHistoryRepository;
import com.plover.backerymanagmentsystem.pos.repository.SpecialOrderRepository;
import com.plover.backerymanagmentsystem.pos.repository.SaleRepository;
import com.plover.backerymanagmentsystem.manager.model.Customer;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;

@SpringBootApplication
public class BackerymanagmentsystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackerymanagmentsystemApplication.class, args);
	}

	@Bean
	public CommandLineRunner cleanAndDeduplicateCustomers(
			CustomerRepository customerRepository,
			CustomerPointsHistoryRepository pointsHistoryRepository,
			SpecialOrderRepository specialOrderRepository,
			SaleRepository saleRepository,
			JdbcTemplate jdbcTemplate) {
		return args -> {
			System.out.println("--- Starting customer NIC cleanup and deduplication ---");

			// 1. Clean empty/blank NIC strings to null, and clean invalid NICs to null
			List<Customer> customers = customerRepository.findAll();
			for (Customer customer : customers) {
				String nic = customer.getIdCardNumber();
				if (nic != null) {
					if (nic.trim().isEmpty()) {
						System.out.println("Setting empty NIC to null for customer: " + customer.getName() + " (ID: " + customer.getId() + ")");
						customer.setIdCardNumber(null);
						customerRepository.save(customer);
					} else if (!isValidNic(nic)) {
						System.out.println("Cleaning invalid NIC: '" + nic + "' to null for customer: " + customer.getName() + " (ID: " + customer.getId() + ")");
						customer.setIdCardNumber(null);
						customerRepository.save(customer);
					}
				}
			}

			// 2. Identify and resolve duplicate NICs
			// Re-fetch all customers to get updated values
			customers = customerRepository.findAll();
			java.util.Map<String, List<Customer>> nicGroups = new java.util.HashMap<>();
			for (Customer c : customers) {
				String nic = c.getIdCardNumber();
				if (nic != null && !nic.trim().isEmpty()) {
					String key = nic.trim().toUpperCase();
					nicGroups.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(c);
				}
			}

			for (java.util.Map.Entry<String, List<Customer>> entry : nicGroups.entrySet()) {
				List<Customer> group = entry.getValue();
				if (group.size() > 1) {
					String nic = entry.getKey();
					System.out.println("Found " + group.size() + " duplicate customers for NIC: " + nic);

					// Sort: highest loyalty points first, then lowest ID (earliest registered)
					group.sort((c1, c2) -> {
						double p1 = c1.getLoyaltyPoints() != null ? c1.getLoyaltyPoints() : 0.0;
						double p2 = c2.getLoyaltyPoints() != null ? c2.getLoyaltyPoints() : 0.0;
						if (p1 != p2) {
							return Double.compare(p2, p1);
						}
						return Long.compare(c1.getId(), c2.getId());
					});

					Customer primaryCustomer = group.get(0);
					System.out.println("Keeping customer: " + primaryCustomer.getName() + " (ID: " + primaryCustomer.getId() + ", Points: " + primaryCustomer.getLoyaltyPoints() + ") as primary.");

					double additionalPoints = 0.0;
					for (int i = 1; i < group.size(); i++) {
						Customer duplicate = group.get(i);
						System.out.println("Merging duplicate customer: " + duplicate.getName() + " (ID: " + duplicate.getId() + ", Points: " + duplicate.getLoyaltyPoints() + ")");
						
						additionalPoints += duplicate.getLoyaltyPoints() != null ? duplicate.getLoyaltyPoints() : 0.0;

						// Relink Points History
						List<com.plover.backerymanagmentsystem.manager.model.CustomerPointsHistory> historyList = pointsHistoryRepository.findByCustomerIdOrderByCreatedAtDesc(duplicate.getId());
						for (com.plover.backerymanagmentsystem.manager.model.CustomerPointsHistory history : historyList) {
							history.setCustomer(primaryCustomer);
							pointsHistoryRepository.save(history);
						}

						// Relink Special Orders
						List<com.plover.backerymanagmentsystem.pos.model.SpecialOrder> specialOrders = specialOrderRepository.findAll();
						for (com.plover.backerymanagmentsystem.pos.model.SpecialOrder order : specialOrders) {
							if (order.getCustomer() != null && order.getCustomer().getId().equals(duplicate.getId())) {
								order.setCustomer(primaryCustomer);
								specialOrderRepository.save(order);
							}
						}

						// Relink Sales
						List<com.plover.backerymanagmentsystem.pos.model.Sale> sales = saleRepository.findAll();
						for (com.plover.backerymanagmentsystem.pos.model.Sale sale : sales) {
							if (duplicate.getId().equals(sale.getCustomerId())) {
								sale.setCustomerId(primaryCustomer.getId());
								saleRepository.save(sale);
							}
						}

						// Delete duplicate customer
						customerRepository.delete(duplicate);
					}

					if (additionalPoints > 0.0) {
						double currentPoints = primaryCustomer.getLoyaltyPoints() != null ? primaryCustomer.getLoyaltyPoints() : 0.0;
						primaryCustomer.setLoyaltyPoints(currentPoints + additionalPoints);
						customerRepository.save(primaryCustomer);
						System.out.println("Added " + additionalPoints + " points to primary customer. New total: " + primaryCustomer.getLoyaltyPoints());
					}
				}
			}

			// 3. Try to programmatically add the unique constraint to the customers table
			try {
				jdbcTemplate.execute("ALTER TABLE customers ADD CONSTRAINT UC_id_card_number UNIQUE (id_card_number)");
				System.out.println("Successfully added unique constraint UC_id_card_number to customers table.");
			} catch (Exception e) {
				System.out.println("Unique constraint UC_id_card_number might already exist: " + e.getMessage());
			}

			// 4. Correct any inflated day_production_items current_qty records
			try {
				int updatedRows = jdbcTemplate.update("UPDATE day_production_items SET current_qty = received_qty WHERE current_qty > received_qty");
				if (updatedRows > 0) {
					System.out.println("Corrected " + updatedRows + " day_production_items where current_qty exceeded received_qty.");
				}
			} catch (Exception e) {
				System.out.println("Error updating day_production_items stock: " + e.getMessage());
			}

			// 5. Add cash_denominations column to shift_closures if missing
			try {
				jdbcTemplate.execute("ALTER TABLE shift_closures ADD COLUMN cash_denominations TEXT");
				System.out.println("Successfully added cash_denominations column to shift_closures table.");
			} catch (Exception e) {
				System.out.println("cash_denominations column check: " + e.getMessage());
			}

			System.out.println("--- Customer NIC cleanup and deduplication complete ---");
		};
	}

	private boolean isValidNic(String nicNumber) {
		if (nicNumber == null || nicNumber.trim().isEmpty()) {
			return true;
		}
		String nic = nicNumber.trim().toUpperCase();
		String yearStr = "";
		String dayStr = "";
		
		if (nic.length() == 10 && nic.substring(0, 9).matches("\\d{9}") && (nic.endsWith("V") || nic.endsWith("X"))) {
			yearStr = "19" + nic.substring(0, 2);
			dayStr = nic.substring(2, 5);
		} else if (nic.length() == 12 && nic.matches("\\d{12}")) {
			yearStr = nic.substring(0, 4);
			dayStr = nic.substring(4, 7);
		} else {
			return false;
		}
		
		try {
			int year = Integer.parseInt(yearStr);
			int dayValue = Integer.parseInt(dayStr);
			
			int daysToAdd = dayValue;
			if (dayValue > 500) {
				daysToAdd = dayValue - 500;
			}
			
			if (daysToAdd < 1 || daysToAdd > 366) {
				return false;
			}
			
			int[] monthLengths = {31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
			int remainingDays = daysToAdd;
			int monthIndex = 0;
			while (monthIndex < 12 && remainingDays > monthLengths[monthIndex]) {
				remainingDays -= monthLengths[monthIndex];
				monthIndex++;
			}
			
			return monthIndex < 12;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}

