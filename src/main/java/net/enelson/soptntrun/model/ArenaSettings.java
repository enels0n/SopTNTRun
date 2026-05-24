package net.enelson.soptntrun.model;

public final class ArenaSettings {

    private int minPlayers;
    private int maxPlayers;
    private int countdownSeconds;
    private int defaultDestroyDelayTicks;
    private int powerupSpawnIntervalSeconds;
    private int powerupDespawnSeconds;
    private double featherJumpVelocity;
    private double dashVelocity;
    private double snowballKnockbackStrength;
    private int maxActivePowerups;
    private double powerupSpawnRadius;
    private PowerupSpawnShape powerupSpawnShape;
    private int winnerFireworksIntervalTicks;
    private double winnerFireworksRadius;

    public ArenaSettings(int minPlayers,
                         int maxPlayers,
                         int countdownSeconds,
                         int defaultDestroyDelayTicks,
                         int powerupSpawnIntervalSeconds,
                         int powerupDespawnSeconds,
                         double featherJumpVelocity,
                         double dashVelocity,
                         double snowballKnockbackStrength,
                         int maxActivePowerups,
                         double powerupSpawnRadius,
                         PowerupSpawnShape powerupSpawnShape,
                         int winnerFireworksIntervalTicks,
                         double winnerFireworksRadius) {
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.countdownSeconds = countdownSeconds;
        this.defaultDestroyDelayTicks = defaultDestroyDelayTicks;
        this.powerupSpawnIntervalSeconds = powerupSpawnIntervalSeconds;
        this.powerupDespawnSeconds = powerupDespawnSeconds;
        this.featherJumpVelocity = featherJumpVelocity;
        this.dashVelocity = dashVelocity;
        this.snowballKnockbackStrength = snowballKnockbackStrength;
        this.maxActivePowerups = maxActivePowerups;
        this.powerupSpawnRadius = powerupSpawnRadius;
        this.powerupSpawnShape = powerupSpawnShape;
        this.winnerFireworksIntervalTicks = winnerFireworksIntervalTicks;
        this.winnerFireworksRadius = winnerFireworksRadius;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getCountdownSeconds() {
        return countdownSeconds;
    }

    public int getDefaultDestroyDelayTicks() {
        return defaultDestroyDelayTicks;
    }

    public int getPowerupSpawnIntervalSeconds() {
        return powerupSpawnIntervalSeconds;
    }

    public int getPowerupDespawnSeconds() {
        return powerupDespawnSeconds;
    }

    public double getFeatherJumpVelocity() {
        return featherJumpVelocity;
    }

    public double getDashVelocity() {
        return dashVelocity;
    }

    public double getSnowballKnockbackStrength() {
        return snowballKnockbackStrength;
    }

    public int getMaxActivePowerups() {
        return maxActivePowerups;
    }

    public double getPowerupSpawnRadius() {
        return powerupSpawnRadius;
    }

    public PowerupSpawnShape getPowerupSpawnShape() {
        return powerupSpawnShape;
    }

    public int getWinnerFireworksIntervalTicks() {
        return winnerFireworksIntervalTicks;
    }

    public double getWinnerFireworksRadius() {
        return winnerFireworksRadius;
    }
}
