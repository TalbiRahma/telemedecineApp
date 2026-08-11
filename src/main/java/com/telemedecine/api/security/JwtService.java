package com.telemedecine.api.security;

import com.telemedecine.api.model.user.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshExpiration;


    public String extractUsername(String jwtToken) {
        return extractClaim(jwtToken, Claims::getSubject);
    }

    public <T> T extractClaim(String jwtToken, Function<Claims, T> claimsResolver) {
        final Claims claims = extractClaimsJWT(jwtToken);
        return claimsResolver.apply(claims);
    }


    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails
    ) {
        extraClaims.put("tokenType", "ACCESS");
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    public String generateRefreshToken(
            UserDetails userDetails
    ) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenType", "REFRESH");
        return buildToken(claims, userDetails, refreshExpiration);
    }

    public boolean isAccessToken(String jwtToken) {
        return "ACCESS".equals(extractClaim(jwtToken, claims -> claims.get("tokenType", String.class)));
    }

    public boolean isRefreshToken(String jwtToken) {
        return "REFRESH".equals(extractClaim(jwtToken, claims -> claims.get("tokenType", String.class)));
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ){
        // Extract only the ROLE_xxx authority
        String role = userDetails.getAuthorities()
                .stream()
                .map(auth -> auth.getAuthority())
                .filter(auth -> auth.startsWith("ROLE_"))
                .findFirst()
                .orElse(null);


        Long userId = null;
        int authenticationVersion = 0;
        if (userDetails instanceof UserEntity) {
            UserEntity user = (UserEntity) userDetails;
            userId = user.getId();
            authenticationVersion = user.getAuthenticationVersion() == null
                    ? 0 : user.getAuthenticationVersion();
        }

        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .claim("role", role != null ? role.replace("ROLE_", "") : null)
                .claim("id", userId)
                .claim("authVersion", authenticationVersion)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact(); // generate and return the token
    }

    public boolean isTokenValid(String jwtToken, UserDetails userDetails) {
        final String username = extractUsername(jwtToken);
        return username.equals(userDetails.getUsername())
                && !isTokenExpired(jwtToken)
                && hasCurrentAuthenticationVersion(jwtToken, userDetails);
    }

    private boolean hasCurrentAuthenticationVersion(String jwtToken, UserDetails userDetails) {
        if (!(userDetails instanceof UserEntity user)) {
            return true;
        }
        Integer tokenVersion = extractClaim(jwtToken, claims -> claims.get("authVersion", Integer.class));
        int currentVersion = user.getAuthenticationVersion() == null ? 0 : user.getAuthenticationVersion();
        int issuedVersion = tokenVersion == null ? 0 : tokenVersion;
        return issuedVersion == currentVersion;
    }

    private boolean isTokenExpired(String jwtToken) {
        return extractExpiration(jwtToken).before(new Date());
    }

    private Date extractExpiration(String jwtToken) {
      return extractClaim(jwtToken, Claims::getExpiration);
    }

    private Claims extractClaimsJWT(String jwtToken) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(jwtToken)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
