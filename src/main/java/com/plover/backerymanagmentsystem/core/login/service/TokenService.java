package com.plover.backerymanagmentsystem.core.login.service;

import com.plover.backerymanagmentsystem.core.login.dto.AuthResponseDto;
import com.plover.backerymanagmentsystem.core.login.model.AuthModel;

public interface TokenService {

    AuthResponseDto generateTokens(AuthModel user);

    AuthResponseDto refreshTokens(String refreshToken);
}
