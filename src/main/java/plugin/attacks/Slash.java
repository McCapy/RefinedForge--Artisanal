package plugin.attacks;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import plugin.combomanager.ComboState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class Slash extends Attack {

    public boolean blank;
    public int rotation;
    public float yaw;
    public float pitch;
    public double arcRadius;
    public int detail;
    public int layers;
    public double layerSpace;
    public int delay;

    public List<String> layerColors = new ArrayList<>();
    public List<Color> parsedLayerColors = new ArrayList<>();

    public List<Float> particleSize = new ArrayList<>();
    public double damage;
    public double cooldown;
    public int jitter;
    public double distanceFromPlayer;
    public List<String> sound = new ArrayList<>();
    public double originAboveHead;
    public double horizontalRotation;
    public double horizontalOffset;
    public double damageRegion;
    public AttackType attackType;

    public Slash() {}

    public void setLayerColors(List<String> layerColors) {
        this.layerColors = layerColors;
    }

    public void setParticleSize(List<Float> particleSize) {
        this.particleSize = particleSize;
    }

    public void setBlank(boolean blank) { this.blank = blank; }
    public void setRotation(int rotation) { this.rotation = rotation; }
    public void setArcRadius(double arcRadius) { this.arcRadius = arcRadius; }
    public void setDetail(int detail) { this.detail = detail; }
    public void setLayers(int layers) { this.layers = layers; }
    public void setLayerSpace(double layerSpace) { this.layerSpace = layerSpace; }
    public void setDelay(int delay) { this.delay = delay; }
    public void setDamage(double damage) { this.damage = damage; }
    public void setCooldown(double cooldown) { this.cooldown = cooldown; }
    public void setJitter(int jitter) { this.jitter = jitter; }
    public void setDistanceFromPlayer(double distanceFromPlayer) { this.distanceFromPlayer = distanceFromPlayer; }
    public void setSound(List<String> sound) { this.sound = sound; }
    public void setOriginAboveHead(double originAboveHead) { this.originAboveHead = originAboveHead; }
    public void setHorizontalRotation(double horizontalRotation) { this.horizontalRotation = horizontalRotation; }
    public void setHorizontalOffset(double horizontalOffset) { this.horizontalOffset = horizontalOffset; }
    public void setDamageRegion(double damageRegion) { this.damageRegion = damageRegion; }

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

    public List<Vector> getArc(Vector ref, Vector base) {
        Vector finalBase = base.clone();
        Vector finalRef = ref.clone();
        int newRotation = rotation + new Random().nextInt(jitter * -1, jitter + 1);
        List<Vector> finish = new ArrayList<>(detail);
        double tempRotationAmount = Math.toRadians(360d / detail);
        double tempHorizontalRotation = Math.toRadians(horizontalRotation);

        for (int i = 0; i < detail; i++) {
            ref.rotateAroundAxis(base, Math.toRadians(newRotation));
            base.rotateAroundAxis(ref, 1.57079633 + tempHorizontalRotation);
            finish.add(base.rotateAroundAxis(ref, (tempRotationAmount * i) / -2));
            ref = finalRef.clone();
            base = finalBase.clone();
        }
        return finish;
    }

    @Override
    public void attack(Player p, ComboState state) {
        if (blank) return;

        float yaw = p.getYaw();
        float pitch = p.getPitch();

        this.yaw = yaw;
        this.pitch = pitch;

        Vector base = new Location(null, 0, 0, 0, yaw, pitch).getDirection().normalize();
        Vector referenceBase = new Location(null, 0, 0, 0, yaw, pitch - 90).getDirection().normalize();

        this.parsedLayerColors = toColor(this.layerColors);

        Vector offset = this.horizontalOffset >= 0
                ? base.clone().rotateAroundAxis(referenceBase.clone(), 90)
                : base.clone().rotateAroundAxis(referenceBase.clone(), -90);

        List<Vector> vectors = getArc(referenceBase, base);

        int fixedLayerCount = layers;
        double fixedArcRadius = arcRadius;
        double fixedLayerSpace = layerSpace;
        double dmg = Math.clamp((state.timeSince) / cooldown, 0, 1) * damage;
        AtomicBoolean prevent = new AtomicBoolean(false);

        Location start = (p.getEyeLocation()
                .add(0, this.originAboveHead, 0)
                .add(p.getEyeLocation().getDirection().multiply(distanceFromPlayer)))
                .add(offset.normalize().multiply(Math.abs(this.horizontalOffset)));

        World world = start.getWorld();

        world.playSound(start, sound.getFirst(), Float.parseFloat(sound.get(1)), Float.parseFloat(sound.getLast()));

        for (int i = 0; i < detail / delay; i++) {
            int fixedI = i;
            Bukkit.getScheduler().runTaskLater(plugin.RefinedForgeArtisanal.getPlugin(), () -> {
                for (int j = 0; j < delay; j++) {
                    for (int k = 0; k < fixedLayerCount; k++) {
                        Particle.DustOptions dustOptions =
                                new Particle.DustOptions(parsedLayerColors.get(k), particleSize.get(k));

                        double currentLayerSpace = fixedArcRadius + (k * fixedLayerSpace);
                        Vector currentVec = vectors.get(fixedI * delay + j);
                        Location location = start.clone().add(currentVec.clone().normalize().multiply(currentLayerSpace));

                        if (k % 2 != 0 && j % 2 == 0 && !prevent.get()) {
                            world.getNearbyEntities(location, this.damageRegion, this.damageRegion, this.damageRegion)
                                    .forEach(entity -> {
                                        if (entity instanceof LivingEntity living && !entity.getName().equals(p.getName())) {
                                            living.setNoDamageTicks(0);
                                            living.damage(dmg, p);
                                            living.setNoDamageTicks(0);
                                            prevent.set(true);
                                        }
                                    });
                        }

                        world.spawnParticle(Particle.DUST, location, 1, dustOptions);
                    }
                }
            }, i);
        }
    }
}
