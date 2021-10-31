package com.theswagbois.towerdefense.components;

import com.almasb.fxgl.core.math.FXGLMath;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.dsl.components.ProjectileComponent;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.time.LocalTimer;
import com.theswagbois.towerdefense.TowerDefenseType;
import com.theswagbois.towerdefense.services.TowerData;
import javafx.geometry.Point2D;
import javafx.util.Duration;

public class TowerComponent extends Component {

    private int damage;
    private int hp;
    private double attackDelay;
    private double accuracy;
    private double bulletSpeed;
    private LocalTimer shootTimer;
    private double accuracyError = 1;

    public TowerComponent(int index) {
        TowerData td = TowerData.getTowersData().get(index - 1);
        this.damage = td.getDamage();
        this.hp = td.getHp();
        this.attackDelay = td.getAttackDelay();
        this.accuracy = td.getAccuracy();
        this.bulletSpeed = td.getBulletSpeed();
    }

    @Override
    public void onAdded() {
        shootTimer = FXGL.newLocalTimer();
        shootTimer.capture();
    }

    @Override
    public void onUpdate(double tpf) {

        if (shootTimer.elapsed(Duration.seconds(attackDelay))) {
            FXGL.getGameWorld()
                    .getClosestEntity(entity, e -> e.isType(TowerDefenseType.ENEMY))
                    .ifPresent(nearestEnemy -> {
                        shoot(nearestEnemy);
                        shootTimer.capture();
                    });
        }
    }

    private void shoot(Entity enemy) {
        double bulletSpeed = this.bulletSpeed;
        boolean smartAiming = false;

        EnemyComponent enemyProjectile = enemy.getComponent(EnemyComponent.class);
        Point2D position = getEntity().getPosition();
        Point2D enemyPosition = enemy.getPosition();
        accuracyError = accuracyError + FXGLMath.random(-(1-accuracy), 1-accuracy);
        Point2D enemyVelocity = enemyProjectile.getVelocity().multiply(bulletSpeed/5).multiply(accuracyError);

        // Point intercept(Point const &shooter, double bullet_speed, Point const &target, Vector const &target_velocity){
        // double a = bullet_speed*bullet_speed - target_velocity.dot(target_velocity);
        // double b = -2*target_velocity.dot(target-shooter);
        // double c = -(target-shooter).dot(target-shooter);
        // return target+largest_root_of_quadratic_equation(a,b,c)*target_velocity;
        // }

        // double largest_root_of_quadratic_equation(double A, double B, double C){
        // return (B+std::sqrt(B*B-4*A*C))/(2*A);
        // }


        double a = enemyVelocity.getX() * enemyVelocity.getX() + enemyVelocity.getY() * enemyVelocity.getY() - bulletSpeed * bulletSpeed;
        double b = 2 * (enemyVelocity.getX() * (enemyPosition.getX() - position.getX()) + enemyPosition.getY() * (enemyPosition.getY() - position.getY()));
        double c = (enemyPosition.getX() - position.getX()) * (enemyPosition.getX() - position.getX()) + (enemyPosition.getY() - position.getY()) * (enemyPosition.getY() - position.getY());
        double disc = b * b - 4 * a * c;
        double t1 = (-b + Math.sqrt(disc)) / (2 * a);
        double t2 = (-b - Math.sqrt(disc)) / (2 * a);
        double t;
        if (t1 < 0) {
            t = t2;
        } else if (t2 < 0) {
            t = t1;
        } else t = Math.min(t1, t2);

        double aimX = t * enemyVelocity.getX() + enemyPosition.getX();
        double aimY = t * enemyVelocity.getY() + enemyPosition.getY();
        Point2D aim = new Point2D(aimX, aimY);

        Entity bullet = FXGL.spawn("Bullet", position);
        bullet.addComponent(new ProjectileComponent(aim.subtract(position).normalize(), bulletSpeed));
        bullet.addComponent(new BulletComponent(damage));
    }
}
