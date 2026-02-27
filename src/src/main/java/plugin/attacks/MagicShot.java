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

public class MagicShot extends Attack {

    public AttackType attackType;
    public int spiralQuantity;
    public int length;
    public double radius;
    public double step;
    public double radiusFallOff;
    public boolean blank;
    public double originAboveHead;
    public List<String> gradientRaw = new ArrayList<>();
    public transient List<Color> parsedGradient = new ArrayList<>();
    public double damageRegion;
    public double damage;
    public double cooldown;
    public double tempRad;
    public List<String> sound = new ArrayList<>(3);

    public MagicShot() {}

    public void setAttackType(String attackType) {
        this.attackType = AttackType.valueOf(attackType);
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

    public void setSpiralQuantity(int spiralQuantity) { this.spiralQuantity = spiralQuantity; }
    public void setSound(List<String> sound) { this.sound = sound; }
    public void setCooldown(double cooldown) { this.cooldown = cooldown; }
    public void setDamage(double damage) { this.damage = damage; }
    public void setDamageRegion(double damageRegion) { this.damageRegion = damageRegion; }
    public void setGradient(List<String> gradientRaw) {
        this.gradientRaw = gradientRaw;
    }
    public void setOriginAboveHead(double originAboveHead) { this.originAboveHead = originAboveHead; }
    public void setBlank(boolean blank) { this.blank = blank; }
    public void setRadiusFallOff(double radiusFallOff) { this.radiusFallOff = radiusFallOff; }
    public void setStep(double step) { this.step = step; }
    public void setRadius(double radius) {
        this.radius = radius;
    }
    public void setLength(int length) { this.length = length; }

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

    @Override
    public void attack(Player player, ComboState state) {

        if (this.blank) return;

        Location refLoc = player.getEyeLocation().add(0, originAboveHead, 0);

        float yaw = player.getYaw();
        float pitch = player.getPitch();

        World world = player.getWorld();

        Vector base = player.getLocation().getDirection().normalize();
        Vector ref = new Location(world, 0, 0, 0, yaw + 90, pitch).getDirection().normalize();

        final double rotAmt = Math.toRadians(360d / spiralQuantity);
        if (tempRad == 0.0d) {
            tempRad = radius;
        }
        radius = tempRad;

        this.parsedGradient = interpolate(this.length,toColor(this.gradientRaw));

        List<Vector> ring = new ArrayList<>(spiralQuantity);
        for (int i = 0; i < spiralQuantity; i++) ring.add(ref.clone().rotateAroundAxis(base, rotAmt * i));

        AtomicBoolean stop = new AtomicBoolean(false);
        double dmg = Math.clamp((state.timeSince) / cooldown, 0, 1) * damage;

        world.playSound(refLoc, sound.getFirst(), Float.parseFloat(sound.get(1)), Float.parseFloat(sound.getLast()));

        for (int i = 0; i < length; i++) {
            final int finalI = i;
            Bukkit.getScheduler().runTaskLater(RefinedForgeArtisanal.getPlugin(),() -> {
                radius += radiusFallOff;
                if (radius <= 0) return;
                double spiralRot = Math.toRadians(finalI * (360d / length));
                Location loc = refLoc.clone().add(base.clone().multiply(finalI * step));

                Collection<Entity> victims = world.getNearbyEntities(loc, this.damageRegion, this.damageRegion, this.damageRegion);

                for (Entity victim : victims) {
                    if (victim instanceof LivingEntity && !victim.getName().equals(player.getName())) {
                        if (!stop.get()) {
                            stop.set(true);
                            ((LivingEntity) victim).damage(dmg);
                        }
                    }
                }

                for (Vector v : ring) {

                    Vector rotated = v.clone().rotateAroundAxis(base, spiralRot).multiply(radius);
                    Location tempLoc = loc.clone().add(rotated);

                    int index = Math.min(finalI, parsedGradient.size());

                    Particle.DUST.builder()
                            .location(tempLoc)
                            .count(1)
                            .color(parsedGradient.get(index),1.2f)
                            .spawn();
                }
            },i);
        }

    }
}
