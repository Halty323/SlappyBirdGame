package com.halty.slappybird;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

import java.util.Random;

public class Pillar {
    public static final float WIDTH = 1;
    public static final float HEIGHT = 5;
    private Body pillarBody;

    public Pillar(float x, float y, World world) {
        BodyDef pillarBodyDef = new BodyDef();
        pillarBodyDef.type = BodyDef.BodyType.KinematicBody;
        pillarBodyDef.position.set(x, y);

        pillarBody = world.createBody(pillarBodyDef);
        PolygonShape pillarShape = new PolygonShape();

        pillarShape.setAsBox(WIDTH, HEIGHT);
        pillarBody.createFixture(pillarShape, 0.0f);
        pillarShape.dispose();
    }

    public void setPosition(float x) {
        pillarBody.setTransform(x, pillarBody.getPosition().y, 0);
    }

    public void setPosition(float x, float y) {
        pillarBody.setTransform(x, y, 0);
    }

    public Vector2 getPosition() {
        return pillarBody.getPosition();
    }

    public void respawn(float x, Pillar pillar2) {
        float randY = new Random().nextFloat() * Pillar.HEIGHT; // Случайное значение Y
        pillarBody.setTransform(x, randY, 0); // Перемещение колонны
        pillar2.setPosition(x, randY + Pillar.HEIGHT + 9);
    }

}
