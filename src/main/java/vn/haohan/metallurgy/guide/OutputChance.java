package vn.haohan.metallurgy.guide;

/** Runtime clean-metal/slag chances with and without the universal Borax flux. */
public record OutputChance(double cleanWithoutBorax, double cleanWithBorax, int boraxAmount) {
    public OutputChance {
        cleanWithoutBorax = clamp(cleanWithoutBorax);
        cleanWithBorax = clamp(cleanWithBorax);
        boraxAmount = Math.max(1, boraxAmount);
    }

    public double slagWithoutBorax() {
        return 1.0 - cleanWithoutBorax;
    }

    public double slagWithBorax() {
        return 1.0 - cleanWithBorax;
    }

    public int cleanWithoutBoraxPercent() {
        return percent(cleanWithoutBorax);
    }

    public int cleanWithBoraxPercent() {
        return percent(cleanWithBorax);
    }

    public int slagWithoutBoraxPercent() {
        return percent(slagWithoutBorax());
    }

    public int slagWithBoraxPercent() {
        return percent(slagWithBorax());
    }

    private static int percent(double value) {
        return (int) Math.round(clamp(value) * 100.0);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
