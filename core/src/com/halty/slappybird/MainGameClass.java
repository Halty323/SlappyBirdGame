package com.halty.slappybird;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

import java.util.Random;


public class MainGameClass extends ApplicationAdapter {

	SpriteBatch batch;
	Texture birdTexture;
	Texture grassTexture;
	Texture backgroundTexture;
	Texture pillarTexture;
	World world;
	Body birdBody;

	Body groundBody;
	Body ceilingBody;

	Box2DDebugRenderer debugRenderer;
	OrthographicCamera camera;
	Sound jump;
	Sound death;

	private float backgroundX = 0; // Координата X фона
	private boolean isGameOver = false;
	private Pillar[] pillars;

	private final float backgroundSpeed = 2;
	private final float gap = 9;
	private final float spaceBetweenPillars = 3.5f;
	private final float pillarSpeed = 3;

	@Override
	public void create() {
		batch = new SpriteBatch();
		// Загрузка текстур
		birdTexture = new Texture("bird1.png");
		grassTexture = new Texture("grass.png");
		backgroundTexture = new Texture("background.png");
		pillarTexture = new Texture("pillar.png");
		grassTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

		// 1. Создаём мир с гравитацией
		world = new World(new Vector2(0, -10f), true);
		debugRenderer = new Box2DDebugRenderer();

		jump = Gdx.audio.newSound(Gdx.files.internal("jump.mp3"));
		death = Gdx.audio.newSound(Gdx.files.internal("die.mp3"));

		// 2. Создаём тело шарика
		BodyDef ballBodyDef = new BodyDef();
		ballBodyDef.type = BodyDef.BodyType.DynamicBody;
		ballBodyDef.position.set(10, 10); // Позиция в метрах
		birdBody = world.createBody(ballBodyDef);

		// 3. Создаём форму (круг) для шарика
		CircleShape circleShape = new CircleShape();
		circleShape.setRadius(0.5f); // Радиус в метрах

		// 4. Создаём фикстуру, соединяющую тело и форму
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = circleShape;
		fixtureDef.density = 1f; // Плотность
		fixtureDef.restitution = 0.2f; // Коэффициент упругости (отскок)
		birdBody.createFixture(fixtureDef);
		circleShape.dispose();

		Gdx.input.setInputProcessor(new InputAdapter() {
			@Override
			public boolean keyDown(int keycode) {
				if (keycode == Input.Keys.SPACE) {
					birdBody.setLinearVelocity(new Vector2(0, 6f));
					jump.play(0.1f);
				}
				return true;
			}
		});

		// 5. Создаём пол и потолок
		BodyDef groundBodyDef = new BodyDef();
		groundBodyDef.type = BodyDef.BodyType.StaticBody;
		groundBodyDef.position.set(0, 0);

		BodyDef ceilingBodyDef = new BodyDef();
		ceilingBodyDef.type = BodyDef.BodyType.StaticBody;
		ceilingBodyDef.position.set(0, 15);

		groundBody = world.createBody(groundBodyDef);
		ceilingBody = world.createBody(ceilingBodyDef);

		PolygonShape groundShape = new PolygonShape();
		PolygonShape ceilingShape = new PolygonShape();

		groundShape.setAsBox(50, 2); // Половина ширины и высоты
		groundBody.createFixture(groundShape, 0.0f);
		groundShape.dispose();

		ceilingShape.setAsBox(50, 0.5f); // Половина ширины и высоты
		ceilingBody.createFixture(ceilingShape, 0.0f);
		ceilingShape.dispose();

		// создаем изначальные 6 колонн
		pillars = new Pillar[6];
		for (int i = 0; i < pillars.length; i += 2) {
			float randY =  new Random().nextFloat() * Pillar.HEIGHT;
			pillars[i] = new Pillar( (i + 1) * spaceBetweenPillars + 20, randY, world);
			pillars[i+1] = new Pillar( (i + 1) * spaceBetweenPillars + 20, randY + Pillar.HEIGHT + gap, world);
		}

		// Обработка столкновений
		world.setContactListener(new ContactListener() {
			@Override
			public void beginContact(Contact contact) {
				// Логика при начале столкновения
                try {
                    gameOver();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

			@Override
			public void endContact(Contact contact) {}
			@Override
			public void preSolve(Contact contact, Manifold manifold) {}
			@Override
			public void postSolve(Contact contact, ContactImpulse contactImpulse) {}
		});


		// 6. Настраиваем камеру
		camera = new OrthographicCamera(20, 15); // Размер мира в метрах
		camera.position.set(10f, 7.5f, 0);
		camera.update();
	}

	@Override
	public void render() {
		float delta = Gdx.graphics.getDeltaTime();

		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// 1. Обновляем физический мир
		if (!isGameOver) {
			backgroundX -= backgroundSpeed * delta;
			// Если фон выходит за пределы экрана, переместим его обратно
			if (backgroundX <= -20) {
				backgroundX = 0;
			}
			movePillarsLeft(delta);
			world.step(1 / 60f, 6, 2);
		}
		// 2. Получаем позицию и скорость птицы
		Vector2 birdPosition = birdBody.getPosition();
		Vector2 velocity = birdBody.getLinearVelocity();
		float verticalVelocity = velocity.y;

		// 3. Рассчитываем угол поворота
		float angle = Math.max(verticalVelocity * 10, -60);

		// 4. Устанавливаем угол поворота
		birdBody.setTransform(birdPosition, (float) Math.toRadians(angle));

		float angleDegrees = (float) Math.toDegrees(birdBody.getAngle());

		// 2. Отрисовываем шарик с поворотом
		batch.setProjectionMatrix(camera.combined);
		batch.begin();

		batch.draw(backgroundTexture, backgroundX, 0, 20, 15);
		batch.draw(backgroundTexture, backgroundX + 20, 0, 20, 15);

		batch.draw(birdTexture,
				birdPosition.x - 0.5f, birdPosition.y - 0.5f, // Позиция
				0.5f, 0.5f, // Точка привязки (центр текстуры)
				1, 1, // Ширина и высота
				1.4f, 1, // Масштаб по x и y
				angleDegrees, // Угол поворота (в градусах)
				0, 0, // Часть текстуры для отрисовки (вся текстура)
				birdTexture.getWidth(), birdTexture.getHeight(), // Ширина и высота части текстуры
				false, false // Flip по x и y (не переворачиваем)
		);

		for (int i = 0; i < pillars.length; i += 2) {
			Pillar pillar1 = pillars[i];
			Pillar pillar2 = pillars[i+1];

			Vector2 position1 = pillar1.getPosition();
			Vector2 position2 = pillar2.getPosition();
			batch.draw(pillarTexture, position1.x - Pillar.WIDTH, position1.y - Pillar.HEIGHT, Pillar.WIDTH * 2, Pillar.HEIGHT * 2);
			batch.draw(pillarTexture, position2.x - Pillar.WIDTH, position2.y + Pillar.HEIGHT, Pillar.WIDTH * 2, -Pillar.HEIGHT * 2);
		}

		for (int i = 0; i < Gdx.graphics.getWidth() / grassTexture.getWidth() + 1; i++) {
			batch.draw(grassTexture, grassTexture.getWidth() * i, 0, grassTexture.getWidth() * i  + grassTexture.getWidth(), 2);
		}

		batch.end();

		// 4. (опционально) Отрисовываем отладочную информацию Box2D
		//debugRenderer.render(world, camera.combined);
	}

	private void movePillarsLeft(float delta) {
		for (int i = 0; i < pillars.length; i += 2) {
			Pillar pillar1 = pillars[i];
			Pillar pillar2 = pillars[i+1];

			Vector2 position = pillar1.getPosition();
			float newX = position.x - pillarSpeed * delta; // Новая позиция по X
			if (newX + Pillar.WIDTH < 0) { // Если колонна вышла за пределы экрана
				pillar1.respawn(20, pillar2); // Перемещаем за пределы экрана
			} else {
				pillar1.setPosition(newX);
				pillar2.setPosition(newX);
			}
		}
	}

	private void gameOver() throws InterruptedException {
		isGameOver = true;
		world.setGravity(new Vector2(0, 0)); // Останавливаем гравитацию
		death.play(0.3f);
		Thread.sleep(3000);
		Gdx.app.exit();
	}


	@Override
	public void dispose() {
		batch.dispose();
		birdTexture.dispose();
		world.dispose();
		debugRenderer.dispose();
		backgroundTexture.dispose();
		grassTexture.dispose();
		pillarTexture.dispose();
	}
}
