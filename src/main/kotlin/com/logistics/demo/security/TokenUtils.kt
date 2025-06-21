import io.jsonwebtoken.Jwts
import java.util.Date

object TokenUtils {

    fun generateToken(clientId: String): String {
        val now = Date()
        val expiry = Date(now.time + 3600_000) // 1 hour expiration

        // Generate JWT token
        return Jwts.builder()
            .setSubject(clientId)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .compact()
    }

    fun extractClientIdFromJwt(authorizationHeader: String): String? {
        val token = authorizationHeader.removePrefix("Bearer ").trim()
        val claims = Jwts.parser()
            .parseClaimsJwt(token)
            .body
        return claims["sub"] as? String
    }
}