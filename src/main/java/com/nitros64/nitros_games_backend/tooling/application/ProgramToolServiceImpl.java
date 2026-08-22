package com.nitros64.nitros_games_backend.tooling.application;

import java.util.List;

import org.springframework.stereotype.Service;

import com.nitros64.nitros_games_backend.tooling.domain.ProgramToolType;
import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingTool;
import com.nitros64.nitros_games_backend.shared.persistence.BaseRepository;
import com.nitros64.nitros_games_backend.shared.application.BaseServiceImpl;

import jakarta.transaction.Transactional;

@Service
public class ProgramToolServiceImpl extends BaseServiceImpl<ProgrammingTool,Long> implements ProgramToolService {

    private final ProgramToolTypeService toolTypeService;

    public ProgramToolServiceImpl(
            BaseRepository<ProgrammingTool,Long> baseRepository,
            ProgramToolTypeService toolTypeService) {
        super(baseRepository);
        this.toolTypeService = toolTypeService;
    }

    @Override
    @Transactional
    public ProgrammingTool create(SaveProgrammingToolCommand command) {
        return baseRepository.save(toEntity(command));
    }

    @Override
    @Transactional
    public List<ProgrammingTool> createAll(List<SaveProgrammingToolCommand> commands) {
        List<ProgrammingTool> entities = commands.stream().map(this::toEntity).toList();
        return baseRepository.saveAll(entities);
    }

    @Override
    @Transactional
    public ProgrammingTool updateFromCommand(Long id, SaveProgrammingToolCommand command) {
        ProgrammingTool entity = findById(id);
        entity.setName(command.name());
        entity.setWebPage(command.webPage());
        entity.setImagefilePath(command.imagefilePath());
        entity.setToolType(toolTypeService.findById(command.toolTypeId()));
        return baseRepository.save(entity);
    }

    private ProgrammingTool toEntity(SaveProgrammingToolCommand command) {
        ProgramToolType toolType = toolTypeService.findById(command.toolTypeId());
        return new ProgrammingTool(
                command.name(),
                command.webPage(),
                command.imagefilePath(),
                toolType);
    }
}
