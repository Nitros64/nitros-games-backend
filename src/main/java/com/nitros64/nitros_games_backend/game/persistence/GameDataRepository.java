package com.nitros64.nitros_games_backend.game.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nitros64.nitros_games_backend.game.domain.GameData;

public interface GameDataRepository extends JpaRepository<GameData, Long> {

    @Query("""
            select distinct game
            from GameData game
            left join fetch game.genres
            order by game.id
            """)
    List<GameData> findAllDetailed();

    @Query("""
            select distinct game
            from GameData game
            left join fetch game.genres
            where game.id = :id
            """)
    Optional<GameData> findDetailedById(@Param("id") Long id);

    @Query("""
            select distinct game
            from GameData game
            left join fetch game.genres
            where game.id in :ids
            """)
    List<GameData> findDetailedByIdIn(@Param("ids") Collection<Long> ids);

    @Query(
            value = "select game.id from GameData game",
            countQuery = "select count(game) from GameData game")
    Page<Long> findAllIds(Pageable pageable);

    @Query(
            value = """
                    select game.id
                    from GameData game
                    where (:name is null
                           or lower(game.name) like lower(concat('%', :name, '%')))
                      and (:genreId is null
                           or exists (
                               select genre.id
                               from game.genres genre
                               where genre.id = :genreId
                           ))
                      and (:jam is null or game.jam = :jam)
                    """,
            countQuery = """
                    select count(game)
                    from GameData game
                    where (:name is null
                           or lower(game.name) like lower(concat('%', :name, '%')))
                      and (:genreId is null
                           or exists (
                               select genre.id
                               from game.genres genre
                               where genre.id = :genreId
                           ))
                      and (:jam is null or game.jam = :jam)
                    """)
    Page<Long> searchIds(
            @Param("name") String name,
            @Param("genreId") Long genreId,
            @Param("jam") Boolean jam,
            Pageable pageable);
}
