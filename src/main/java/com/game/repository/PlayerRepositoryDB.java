package com.game.repository;

import com.game.entity.Player;
import com.game.service.PlayerService;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.query.NativeQuery;
import org.springframework.stereotype.Repository;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

@Repository(value = "db")
public class PlayerRepositoryDB implements IPlayerRepository {
    private final SessionFactory sessionFactory;

    public PlayerRepositoryDB() {
        Properties properties = new Properties();

        properties.put(Environment.DRIVER,
                "com.p6spy.engine.spy.P6SpyDriver");

        properties.put(Environment.URL,
                "jdbc:p6spy:mysql://localhost:3306/rpg");

        properties.put(Environment.USER, "root");
        properties.put(Environment.PASS, "admin");
        properties.put(Environment.DIALECT,
                "org.hibernate.dialect.MySQL8Dialect");
        properties.put(Environment.HBM2DDL_AUTO, "update");

        sessionFactory = new Configuration()
                .addAnnotatedClass(Player.class)
                .addProperties(properties)
                .buildSessionFactory();
    }

    @Override
    public List<Player> getAll(int pageNumber, int pageSize) {
        if (pageNumber < 0 || pageSize <= 0){
            throw new IllegalArgumentException(
                    "Номер страницы должен быть >= 0, размер страницы > 0");
        }

        int offset = Math.multiplyExact(pageNumber, pageSize);

        try (Session session = sessionFactory.openSession()){
            NativeQuery<Player> query = session.createNativeQuery(
                    "SELECT * FROM player ORDER BY id LIMIT :limit OFFSET :offset",
                    Player.class
            );
            query.setParameter("limit", pageSize);
            query.setParameter("offset", offset);
            return query.getResultList();
        }
    }

    @Override
    public int getAllCount() {
        try (Session session = sessionFactory.openSession()){
            Long count = session.createNamedQuery(
                    "Player.getAllCount",
                    Long.class
            ).getSingleResult();

            return Math.toIntExact(count);
        }
    }

    @Override
    public Player save(Player player) {
        Transaction transaction = null;

        try (Session session = sessionFactory.openSession()){
            transaction = session.beginTransaction();

            session.persist(player);
            transaction.commit();

            return player;
        } catch (RuntimeException e) {
            rollback(transaction);
            throw e;
        }
    }

    @Override
    public Player update(Player player) {
        Transaction transaction = null;

        try (Session session = sessionFactory.openSession()){
            transaction = session.beginTransaction();

            Player updatedPlayer = (Player) session.merge(player);

            transaction.commit();
            return updatedPlayer;
        } catch (RuntimeException e) {
            rollback(transaction);
            throw e;
        }
    }

    @Override
    public Optional<Player> findById(long id) {
        try (Session session = sessionFactory.openSession()){
            return Optional.ofNullable(session.get(Player.class, id));
        }
    }

    @Override
    public void delete(Player player) {
        Transaction transaction = null;

        try (Session session = sessionFactory.openSession()){
            transaction = session.beginTransaction();

            Player playerToDelete = session.get(Player.class, player.getId());

            if (playerToDelete != null){
                session.remove(playerToDelete);
            }

            transaction.commit();
        } catch (RuntimeException e) {
            rollback(transaction);
            throw e;
        }
    }

    private void rollback(Transaction transaction) {
        if (transaction != null && transaction.isActive()) {
            transaction.rollback();
        }
    }

    @PreDestroy
    public void beforeStop() {
        sessionFactory.close();
    }
}