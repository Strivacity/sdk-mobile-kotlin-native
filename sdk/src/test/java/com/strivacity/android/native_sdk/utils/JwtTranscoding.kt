fun encodeJwtPart(part: String) =
    String(
            java.util.Base64.getUrlEncoder().encode(part.toByteArray()),
        )
        .replace("=", "")
