package com.plover.backerymanagmentsystem.worker.service;

import java.util.List;
import com.plover.backerymanagmentsystem.worker.dto.*;

public interface WorkerKitchenFlowService {
    TransferNoteDto createTransferNote(CreateTransferNoteDto dto);
    List<TransferNoteDto> listMyTransferNotes();
    TransferNoteDto markTransferReceived(Long id);

    KitchenReturnDto createReturn(CreateKitchenReturnDto dto);
    List<KitchenReturnDto> listMyReturns();
}
