package concordia.soen6611.igo_tvm.models;

public enum PassType {
    SINGLE("cardReloadAmount.pass.single"),   // e.g. "Single Pass"
    WEEKLY("cardReloadAmount.pass.weekly"),   // "Weekly Pass"
    MONTHLY("cardReloadAmount.pass.monthly"), // "Monthly Pass"
    DAY("cardReloadAmount.pass.day");         // "Day Pass"

    private final String msgKey;
    PassType(String k) { this.msgKey = k; }
    public String key() { return msgKey; }
}
