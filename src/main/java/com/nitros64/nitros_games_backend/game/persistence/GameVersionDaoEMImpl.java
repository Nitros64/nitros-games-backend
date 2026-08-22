package com.nitros64.nitros_games_backend.game.persistence;

import java.math.BigInteger;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import com.nitros64.nitros_games_backend.game.domain.GameVersion;

@Repository("EntityManagerVersion")
public class GameVersionDaoEMImpl implements IGameVersionDaoEM<GameVersion>
{   
    private EntityManager em;
    
    private static final String JPQL_INSERT = "insert into game_version (name, fk_gamedata, fk_idlang, fk_idtool, fk_idprocessor, fk_idplatform) values (?, ?, ?, ?, ?, ?);";    
    private static final String JPQL_SELECT = "select gv.id from game_version gv where gv.name = ? and gv.fk_gamedata = ? and gv.fk_idlang = ? "
                                            + "and gv.fk_idtool = ? and gv.fk_idprocessor = ? and gv.fk_idplatform = ?";
    
    @PersistenceContext //(type = PersistenceContextType.TRANSACTION)
    public void setEm(EntityManager em) {
        this.em = em;
    }
     
    @Override
    public GameVersion save(GameVersion arg0){        
        em.createNativeQuery(JPQL_INSERT, GameVersion.class)
                .setParameter(1, arg0.getName())
                .setParameter(2, arg0.getGamedata().getId())
                .setParameter(3, arg0.getLang_tool().getProgram_language().getId())
                .setParameter(4, arg0.getLang_tool().getProgram_tool().getId())
                .setParameter(5, arg0.getToolprocessor().getProcessor().getId())
                .setParameter(6, arg0.getToolplatform().getPlatform().getId())
                .executeUpdate();
        
        return this.getID(arg0);
    }
    
    @Override
    public GameVersion getID(GameVersion arg0){
        Query q = em.createNativeQuery(JPQL_SELECT)
                .setParameter(1, arg0.getName())
                .setParameter(2, arg0.getGamedata().getId())
                .setParameter(3, arg0.getLang_tool().getProgram_language().getId())
                .setParameter(4, arg0.getLang_tool().getProgram_tool().getId())
                .setParameter(5, arg0.getToolprocessor().getProcessor().getId())
                .setParameter(6, arg0.getToolplatform().getPlatform().getId());
        
        arg0.setId(((BigInteger) q.getResultList().get(0) ).longValue() );
        return arg0;
    }
    
}
/*
Nombres Potenciales
    * GameVersionDaoEMImpl
    * IGameVersionDaoEM
    * IGameVersionDao_EM
    * EM_IGameVersionDao
    * Dark Shadows
*/
