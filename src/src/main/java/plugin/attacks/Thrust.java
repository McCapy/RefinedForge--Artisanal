package plugin.attacks;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import plugin.RefinedForgeArtisanal;
import plugin.combomanager.ComboState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
public class Thrust extends Attack {

    public void setStartRadius(double startRadius) { this.startRadius = startRadius; }
    public void setCooldown(double cooldown) { this.cooldown = cooldown; }
    public void setThrustStrength(double thrustStrength) { this.thrustStrength = thrustStrength; }
    public void setAttackType(String attackType) { this.attackType = AttackType.valueOf(attackType); }
    public void setThrustDelay(int thrustDelay) { this.thrustDelay = thrustDelay; }
    public void setDamage(double damage) { this.damage = damage; }
    public void setDamageRegion(double damageRegion) { this.damageRegion = damageRegion; }
    public void setBlank(boolean blank) { this.blank = blank; }
    public void setLength(double length) { this.length = length; }
    public void setHandleColor(String handleColorRaw) {
        this.handleColorRaw = handleColorRaw;
        this.handleColor = toColor(handleColorRaw);
    }
    public void setGradient(List<String> gradientRaw) {
        this.gradientRaw = gradientRaw;
        this.parsedGradient = toColor(gradientRaw);
    }

    public double startRadius;
    public List<String> gradientRaw = new ArrayList<>();
    public transient List<Color> parsedGradient = new ArrayList<>();
    public List<String> sound = new ArrayList<>(3);
    public double length;
    public boolean blank;
    public double damageRegion;

    public String handleColorRaw;
    public transient Color handleColor;

    public double damage;
    public int thrustDelay;
    public AttackType attackType;
    public double thrustStrength;
    public double cooldown;

    public Thrust() {}

    public Color toColor(String string) {
        String[] parts = string.replace("rgb(", "").replace(")", "").split(",");
        return Color.fromRGB(
                Integer.parseInt(parts[0].trim()),
                Integer.parseInt(parts[1].trim()),
                Integer.parseInt(parts[2].trim())
        );
    }

    public List<Color> toColor(List<String> strings) {
        List<Color> colors = new ArrayList<>(strings.size());
        for (String string : strings) {
            String[] parts = string.replace("rgb(", "").replace(")", "").split(",");
            colors.add(Color.fromRGB(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim())
            ));
        }
        return colors;
    }

    public static int gradientSteps(double testAmt) {
        int start = (int) Math.round(testAmt * (5d / 12d));
        return (int) testAmt - start;
    }

    private static List<Color> interpolate(int steps, List<Color> gradient) {
        Color start = gradient.getFirst();
        Color end = gradient.getLast();
        if (steps <= 1) return List.of(start);

        List<Color> result = new ArrayList<>(steps);
        for (int i = 0; i < steps; i++) {
            double t = (double) i / (steps - 1);
            int r = (int) (start.getRed() + t * (end.getRed() - start.getRed()));
            int g = (int) (start.getGreen() + t * (end.getGreen() - start.getGreen()));
            int b = (int) (start.getBlue() + t * (end.getBlue() - start.getBlue()));
            result.add(Color.fromRGB(r, g, b));
        }
        return result;
    }

    public void drawThrust(List<Location> locations, Vector base, Vector referenceBase) {

        double loopCount = locations.size();
        double radius = this.startRadius;
        int circleParticles = 15;

        double radiusSubtractRate = (radius / loopCount) * -1;
        double subtractRate = (circleParticles / loopCount) * -1;
        double rotateAmount = 360d / circleParticles;

        List<Color> colors = interpolate(gradientSteps(loopCount), this.parsedGradient);
        int progression = 0;

        locations.getFirst().getWorld().playSound(
                locations.getFirst(),
                sound.getFirst(),
                Float.parseFloat(sound.get(1)),
                Float.parseFloat(sound.getLast())
        );

        for (Location currentLoc : locations) {

            Vector refBase = referenceBase.clone();
            Vector trueBase = base.clone();

            for (int i2 = 0; i2 < circleParticles; i2++) {

                Particle.DUST.builder()
                        .extra(0)
                        .color(colors.get(Math.min(progression, colors.size() - 1)))
                        .location(currentLoc.clone().add(
                                refBase.rotateAroundAxis(trueBase, rotateAmount)
                                        .normalize()
                                        .multiply(radius)
                        ))
                        .force(true)
                        .spawn();
            }

            progression++;
            radius += radiusSubtractRate;
            circleParticles = (int) Math.round(circleParticles + subtractRate);
            rotateAmount = 360d / circleParticles;
        }
    }

    @Override
    public void attack(Player player, ComboState state) {
        if (this.blank) return;

        Vector push = player.getEyeLocation().getDirection().normalize();
        double velocity = Math.clamp((state.timeSince) / cooldown, 0, 1) * thrustStrength;
        player.setVelocity((push.multiply(velocity)));

        World world = player.getWorld();

        Bukkit.getScheduler().runTaskLater(RefinedForgeArtisanal.getPlugin(), () -> {

            double dmg = Math.clamp((state.timeSince) / cooldown, 0, 1) * damage;

            float yaw2 = player.getYaw();
            float pitch2 = player.getPitch();

            Location origin = player.getEyeLocation().add(0, -0.5, 0);

            Vector base = new Location(null, 0, 0, 0, yaw2, pitch2).getDirection().normalize();
            Vector referenceBase = new Location(null, 0, 0, 0, yaw2, pitch2 - 90).getDirection().normalize();

            List<Location> circlePoints = new ArrayList<>();
            int start = (int) Math.round(this.length / 2 * (5 / 6d));

            for (int i = 0; i < this.length; i++) {
                Location line = origin.clone().add(base.clone().multiply(i * 0.25));
                if (i >= start) {
                    circlePoints.add(line);
                } else {
                    Particle.DUST.builder()
                            .color(handleColor)
                            .force(true)
                            .location(line)
                            .spawn();
                }
            }

            AtomicBoolean stop = new AtomicBoolean(false);

            circlePoints.forEach(point -> {
                Collection<Entity> victims = world.getNearbyEntities(
                        point,
                        this.damageRegion,
                        this.damageRegion,
                        this.damageRegion
                );

                for (Entity victim : victims) {
                    if (victim instanceof LivingEntity && !victim.getName().equals(player.getName())) {
                        if (!stop.get()) {
                            stop.set(true);
                            ((LivingEntity) victim).damage(damage);
                        }
                    }
                }
            });

            List<Location> circleDuplicate = new ArrayList<>(circlePoints);

            for (int i = 0; i < circlePoints.size() / 4; i++) {
                circleDuplicate.removeLast();
            }

            circleDuplicate.forEach(location -> Particle.DUST.builder()
                    .color(handleColor)
                    .force(true)
                    .location(location)
                    .spawn());
            world.playSound(origin, sound.getFirst(), Float.parseFloat(sound.get(1)), Float.parseFloat(sound.getLast()));
            drawThrust(circlePoints, base, referenceBase);

        }, thrustDelay);
    }
}
