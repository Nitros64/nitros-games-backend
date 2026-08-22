package com.nitros64.nitros_games_backend.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.nitros64.nitros_games_backend.shared.api.BaseController;
import com.nitros64.nitros_games_backend.shared.api.BaseControllerImpl;
import com.nitros64.nitros_games_backend.shared.api.error.ApiProblem;
import com.nitros64.nitros_games_backend.shared.api.error.ApiProblemWriter;
import com.nitros64.nitros_games_backend.shared.api.error.ApiValidationViolation;
import com.nitros64.nitros_games_backend.shared.api.error.RestExceptionHandler;
import com.nitros64.nitros_games_backend.shared.application.BaseService;
import com.nitros64.nitros_games_backend.shared.application.BaseServiceImpl;
import com.nitros64.nitros_games_backend.shared.application.ResourceNotFoundException;
import com.nitros64.nitros_games_backend.shared.domain.Base;
import com.nitros64.nitros_games_backend.shared.persistence.BaseRepository;
import com.nitros64.nitros_games_backend.shared.validation.NoNumberString;
import com.nitros64.nitros_games_backend.shared.validation.NoNumberValidator;

import jakarta.validation.Valid;

class SharedKernelStructureTests {

    @Test
    void commonAbstractionsStayInsideTheSharedKernel() {
        assertThat(List.of(
                Base.class, BaseRepository.class,
                BaseService.class, BaseServiceImpl.class,
                ResourceNotFoundException.class,
                BaseController.class, BaseControllerImpl.class,
                ApiProblem.class, ApiProblemWriter.class,
                ApiValidationViolation.class, RestExceptionHandler.class,
                NoNumberString.class, NoNumberValidator.class))
                .allSatisfy(type -> assertThat(type.getPackageName())
                        .startsWith("com.nitros64.nitros_games_backend.shared."));
    }

    @Test
    void bulkRequestsValidateEveryElement() throws NoSuchMethodException {
        Method saveAll = BaseController.class.getMethod("saveAll", List.class);
        AnnotatedParameterizedType listType =
                (AnnotatedParameterizedType) saveAll.getAnnotatedParameterTypes()[0];

        assertThat(listType.getAnnotatedActualTypeArguments()[0].getAnnotation(Valid.class))
                .isNotNull();
    }
}
