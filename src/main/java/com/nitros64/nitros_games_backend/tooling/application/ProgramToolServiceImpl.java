package com.nitros64.nitros_games_backend.tooling.application;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nitros64.nitros_games_backend.shared.application.ResourceNotFoundException;
import com.nitros64.nitros_games_backend.tooling.domain.ProgramToolType;
import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingTool;
import com.nitros64.nitros_games_backend.tooling.persistence.ProgrammingToolRepository;

@Service
public class ProgramToolServiceImpl implements ProgramToolService {

    private final ProgrammingToolRepository tools;
    private final ProgramToolTypeService toolTypes;

    public ProgramToolServiceImpl(
            ProgrammingToolRepository tools,
            ProgramToolTypeService toolTypes) {
        this.tools = tools;
        this.toolTypes = toolTypes;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgrammingTool> findAll() {
        return tools.findAllDetailed();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProgrammingTool> findAll(Pageable pageable) {
        return tools.findAllDetailed(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProgrammingTool> search(
            ProgrammingToolSearchCriteria criteria,
            Pageable pageable) {
        return tools.search(
                criteria.name(),
                criteria.toolTypeId(),
                criteria.languageId(),
                criteria.platformId(),
                criteria.processorId(),
                pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ProgrammingTool findById(Long id) {
        return tools.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Programming tool not found"));
    }

    @Override
    @Transactional
    public ProgrammingTool create(SaveProgrammingToolCommand command) {
        return tools.save(toEntity(command));
    }

    @Override
    @Transactional
    public List<ProgrammingTool> createAll(List<SaveProgrammingToolCommand> commands) {
        Map<Long, ProgramToolType> resolvedTypes = toolTypes.findByIds(commands.stream()
                .map(SaveProgrammingToolCommand::toolTypeId)
                .collect(Collectors.toSet()));
        List<ProgrammingTool> entities = commands.stream()
                .map(command -> toEntity(command, resolvedTypes.get(command.toolTypeId())))
                .toList();
        return tools.saveAll(entities);
    }

    @Override
    @Transactional
    public ProgrammingTool updateFromCommand(Long id, SaveProgrammingToolCommand command) {
        ProgrammingTool entity = findById(id);
        entity.updateDetails(
                command.name(),
                command.webPage(),
                command.imagefilePath(),
                toolTypes.findById(command.toolTypeId()));
        return tools.save(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        tools.delete(findById(id));
    }

    private ProgrammingTool toEntity(SaveProgrammingToolCommand command) {
        return toEntity(command, toolTypes.findById(command.toolTypeId()));
    }

    private ProgrammingTool toEntity(
            SaveProgrammingToolCommand command,
            ProgramToolType toolType) {
        return new ProgrammingTool(
                command.name(),
                command.webPage(),
                command.imagefilePath(),
                toolType);
    }
}
