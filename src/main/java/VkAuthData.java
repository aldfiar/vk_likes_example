public class VkAuthData {
    private final Integer clientId;
    private final String secret;
    private final String code;

    public VkAuthData(String clientId, String secret, String code) {
        this.clientId = Integer.parseInt(clientId);
        this.secret = secret;
        this.code = code;
    }

    public Integer getClientId() {
        return clientId;
    }

    public String getSecret() {
        return secret;
    }

    public String getCode() {
        return code;
    }
}
