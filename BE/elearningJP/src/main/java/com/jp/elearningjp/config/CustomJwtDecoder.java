package com.jp.elearningjp.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;

import com.jp.elearningjp.service.auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.text.ParseException;

@Component
public class CustomJwtDecoder implements JwtDecoder {


    @Autowired
    private AuthService authService;

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            // Verify chữ ký + expiry + blacklist
            authService.verifyToken(token, false);

            // Parse thành Jwt object để Spring Security hiểu
            SignedJWT signedJWT = SignedJWT.parse(token);

            return new Jwt(
                    token,
                    signedJWT.getJWTClaimsSet().getIssueTime().toInstant(),
                    signedJWT.getJWTClaimsSet().getExpirationTime().toInstant(),
                    signedJWT.getHeader().toJSONObject(),
                    signedJWT.getJWTClaimsSet().getClaims()
            );

        } catch (ParseException e) {
            throw new JwtException("Token malformed");
        } catch (JOSEException e) {
            throw new JwtException("Token verification failed");
        } catch (Exception e) {
            throw new JwtException("Token invalid: " + e.getMessage());
        }
    }
}