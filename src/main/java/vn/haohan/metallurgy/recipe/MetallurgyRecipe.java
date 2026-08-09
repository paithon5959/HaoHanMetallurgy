package vn.haohan.metallurgy.recipe;

import org.bukkit.Material;

import java.util.List;

/**
 * Đại diện cho một recipe trong hệ thống metallurgy.
 * Được load từ file JSON trong thư mục recipes/.
 * Immutable sau khi tạo — không được thay đổi runtime.
 */
public final class MetallurgyRecipe {

    // ── Fields (khớp với JSON) ─────────────────────────────────
    private final String id;
    private final String machineType;
    private final List<Ingredient> inputs;
    private final OutputItem output;
    private final int fuelCost;
    private final int timeSeconds;
    private final int minTemperature;
    private final int purificationTemperature;
    private final int maxTemperature;
    private final String requiredAdvancement;
    private final double failChance;
    private final double underheatFailChance;
    private final List<Material> requiredAdditives;
    private final int additiveAmount;
    private final double additiveCleanOutputBonus;
    private final List<Flux> fluxes;
    private final boolean requiresColdQuench;
    private final boolean requiresSoulFire;

    public MetallurgyRecipe(String id,
                            String machineType,
                            List<Ingredient> inputs,
                            OutputItem output,
                            int fuelCost,
                            int timeSeconds,
                            int minTemperature,
                            int maxTemperature,
                            String requiredAdvancement) {
        this(id, machineType, inputs, output, fuelCost, timeSeconds, minTemperature,
                minTemperature, maxTemperature, requiredAdvancement, 0.0, 0.0,
                List.of(), 1, -1.0, false, false);
    }

    public MetallurgyRecipe(String id,
                            String machineType,
                            List<Ingredient> inputs,
                            OutputItem output,
                            int fuelCost,
                            int timeSeconds,
                            int minTemperature,
                            int maxTemperature,
                            String requiredAdvancement,
                            double failChance) {
        this(id, machineType, inputs, output, fuelCost, timeSeconds, minTemperature,
                minTemperature, maxTemperature, requiredAdvancement, failChance, failChance,
                List.of(), 1, -1.0, false, false);
    }

    public MetallurgyRecipe(String id,
                            String machineType,
                            List<Ingredient> inputs,
                            OutputItem output,
                            int fuelCost,
                            int timeSeconds,
                            int minTemperature,
                            int maxTemperature,
                            String requiredAdvancement,
                            double failChance,
                            Material requiredAdditive) {
        this(id, machineType, inputs, output, fuelCost, timeSeconds, minTemperature,
                minTemperature, maxTemperature, requiredAdvancement, failChance, failChance,
                requiredAdditive == null ? List.of() : List.of(requiredAdditive), 1, -1.0, false, false);
    }

    public MetallurgyRecipe(String id,
                            String machineType,
                            List<Ingredient> inputs,
                            OutputItem output,
                            int fuelCost,
                            int timeSeconds,
                            int minTemperature,
                            int purificationTemperature,
                            int maxTemperature,
                            String requiredAdvancement,
                            double failChance,
                            double underheatFailChance,
                            List<Material> requiredAdditives,
                            int additiveAmount,
                            double additiveCleanOutputBonus,
                            boolean requiresColdQuench,
                            boolean requiresSoulFire) {
        this.id = id;
        this.machineType = machineType;
        this.inputs = List.copyOf(inputs);
        this.output = output;
        this.fuelCost = fuelCost;
        this.timeSeconds = timeSeconds;
        this.minTemperature = minTemperature;
        this.purificationTemperature = Math.max(minTemperature, purificationTemperature);
        this.maxTemperature = maxTemperature;
        this.requiredAdvancement = requiredAdvancement;
        this.failChance = clampChance(failChance);
        this.underheatFailChance = Math.max(this.failChance, clampChance(underheatFailChance));
        this.requiredAdditives = requiredAdditives == null ? List.of() : List.copyOf(requiredAdditives);
        this.additiveAmount = Math.max(1, additiveAmount);
        this.additiveCleanOutputBonus = additiveCleanOutputBonus < 0.0
                ? -1.0
                : clampChance(additiveCleanOutputBonus);
        this.fluxes = this.requiredAdditives.stream()
                .map(material -> new Flux(material, null, this.additiveAmount,
                        this.additiveCleanOutputBonus))
                .toList();
        this.requiresColdQuench = requiresColdQuench;
        this.requiresSoulFire = requiresSoulFire;
    }

    public MetallurgyRecipe(String id,
                            String machineType,
                            List<Ingredient> inputs,
                            OutputItem output,
                            int fuelCost,
                            int timeSeconds,
                            int minTemperature,
                            int purificationTemperature,
                            int maxTemperature,
                            String requiredAdvancement,
                            double failChance,
                            double underheatFailChance,
                            List<Material> requiredAdditives,
                            int additiveAmount,
                            double additiveCleanOutputBonus,
                            boolean requiresColdQuench,
                            boolean requiresSoulFire,
                            List<Flux> fluxes) {
        this.id = id;
        this.machineType = machineType;
        this.inputs = List.copyOf(inputs);
        this.output = output;
        this.fuelCost = fuelCost;
        this.timeSeconds = timeSeconds;
        this.minTemperature = minTemperature;
        this.purificationTemperature = Math.max(minTemperature, purificationTemperature);
        this.maxTemperature = maxTemperature;
        this.requiredAdvancement = requiredAdvancement;
        this.failChance = clampChance(failChance);
        this.underheatFailChance = Math.max(this.failChance, clampChance(underheatFailChance));
        this.requiredAdditives = requiredAdditives == null ? List.of() : List.copyOf(requiredAdditives);
        this.additiveAmount = Math.max(1, additiveAmount);
        this.additiveCleanOutputBonus = additiveCleanOutputBonus < 0.0
                ? -1.0
                : clampChance(additiveCleanOutputBonus);
        this.fluxes = fluxes == null ? List.of() : List.copyOf(fluxes);
        this.requiresColdQuench = requiresColdQuench;
        this.requiresSoulFire = requiresSoulFire;
    }

    // ── Getters ────────────────────────────────────────────────
    public String getId()            { return id; }
    public String getMachineType()   { return machineType; }
    public List<Ingredient> getInputs() { return inputs; }
    public OutputItem getOutput()    { return output; }
    public int getFuelCost()         { return fuelCost; }
    public int getTimeSeconds()      { return timeSeconds; }
    public int getMinTemperature()   { return minTemperature; }
    public int getPurificationTemperature() { return purificationTemperature; }
    public int getMaxTemperature()   { return maxTemperature; }
    public String getRequiredAdvancement() { return requiredAdvancement; }
    public double getFailChance()    { return failChance; }
    public double getUnderheatFailChance() { return underheatFailChance; }
    public List<Material> getRequiredAdditives() { return requiredAdditives; }
    public int getAdditiveAmount() { return additiveAmount; }
    public double getAdditiveCleanOutputBonus() { return additiveCleanOutputBonus; }
    public List<Flux> getFluxes() { return fluxes; }
    public boolean requiresColdQuench() { return requiresColdQuench; }
    public boolean requiresSoulFire() { return requiresSoulFire; }
    public Material getRequiredAdditive() {
        return requiredAdditives.isEmpty() ? null : requiredAdditives.get(0);
    }

    public boolean acceptsAdditive(Material material) {
        return material != null && (requiredAdditives.contains(material)
                || fluxes.stream().anyMatch(flux -> flux.customItemId() == null
                        && flux.material() == material));
    }

    public double getTemperatureFailChance(int averageTemperature) {
        if (purificationTemperature <= minTemperature || averageTemperature >= purificationTemperature) {
            return failChance;
        }
        if (averageTemperature <= minTemperature) {
            return underheatFailChance;
        }
        double quality = (averageTemperature - minTemperature)
                / (double) (purificationTemperature - minTemperature);
        return underheatFailChance + (failChance - underheatFailChance) * quality;
    }

    /** Extra failure chance caused by running below the refinement temperature. */
    public double getThermalFailurePenalty(int averageTemperature) {
        return Math.max(0.0, getTemperatureFailChance(averageTemperature) - failChance);
    }

    private static double clampChance(double chance) {
        return Math.max(0.0, Math.min(1.0, chance));
    }

    @Override
    public String toString() {
        return "MetallurgyRecipe{id='" + id + "', machine=" + machineType + "}";
    }

    // ── Nested: Ingredient ─────────────────────────────────────

    public record Ingredient(Material material,
                             int amount,
                             String customItemId,
                             List<Material> alternativeMaterials) {
        public Ingredient(Material material, int amount) {
            this(material, amount, null, List.of());
        }

        public Ingredient(Material material, int amount, String customItemId) {
            this(material, amount, customItemId, List.of());
        }

        public Ingredient {
            alternativeMaterials = alternativeMaterials == null
                    ? List.of()
                    : List.copyOf(alternativeMaterials);
        }

        public boolean matches(Material mat, int qty) {
            return (this.material == mat || alternativeMaterials.contains(mat))
                    && qty >= this.amount;
        }

        public boolean matches(org.bukkit.inventory.ItemStack stack) {
            if (stack == null) return false;
            if (stack.getAmount() < this.amount) return false;

            if (customItemId != null && !customItemId.isEmpty()) {
                if (!stack.hasItemMeta()) return false;
                org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
                org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey("haohanmetallurgy", "custom_item_id");
                String id = meta.getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.STRING);
                return customItemId.equalsIgnoreCase(id);
            } else {
                if (stack.hasItemMeta()) {
                    org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
                    org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey("haohanmetallurgy", "custom_item_id");
                    if (meta.getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
                        return false;
                    }
                }
                return stack.getType() == this.material
                        || alternativeMaterials.contains(stack.getType());
            }
        }
    }

    // ── Nested: OutputItem ─────────────────────────────────────

    public record OutputItem(Material material,
                              int amount,
                              String displayName,
                              List<String> lore,
                              int customModelData,
                              String customItemId) {}

    /** Optional flux/catalyst accepted by this recipe. */
    public record Flux(Material material,
                       String customItemId,
                       int amount,
                       double cleanOutputBonus) {
        public Flux {
            if ((customItemId == null || customItemId.isBlank()) && material == null) {
                throw new IllegalArgumentException("flux needs a material or custom item ID");
            }
            customItemId = customItemId == null || customItemId.isBlank() ? null : customItemId;
            if (amount <= 0) throw new IllegalArgumentException("flux amount must be positive");
            if (cleanOutputBonus < -1.0 || cleanOutputBonus > 1.0) {
                throw new IllegalArgumentException("flux clean-output bonus must be -1 or between 0 and 1");
            }
        }

        public boolean matches(org.bukkit.inventory.ItemStack stack) {
            if (stack == null || stack.getAmount() < amount) return false;
            if (customItemId != null) {
                if (!stack.hasItemMeta()) return false;
                org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(
                        "haohanmetallurgy", "custom_item_id");
                String id = stack.getItemMeta().getPersistentDataContainer().get(
                        key, org.bukkit.persistence.PersistentDataType.STRING);
                return customItemId.equalsIgnoreCase(id);
            }
            if (stack.hasItemMeta()) {
                org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(
                        "haohanmetallurgy", "custom_item_id");
                if (stack.getItemMeta().getPersistentDataContainer().has(
                        key, org.bukkit.persistence.PersistentDataType.STRING)) return false;
            }
            return stack.getType() == material;
        }
    }
}
