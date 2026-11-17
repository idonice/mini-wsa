package com.idonice.mini_wsa.service;

import com.idonice.mini_wsa.dto.ingest.EventRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EventValidator {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public EventValidator(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public Result validate(JsonNode item) {
        if (!item.isObject()) {
            return Result.invalid(List.of("event must be a JSON object"));
        }

        EventRequest request;
        try {
            request = objectMapper.treeToValue(item, EventRequest.class);
        } catch (JacksonException ex) {
            return Result.invalid(List.of(describeReadError(ex)));
        }

        List<String> errors = new ArrayList<>();

        for (ConstraintViolation<EventRequest> violation : validator.validate(request)) {
            errors.add(violation.getPropertyPath() + ": " + violation.getMessage());
        }

        if (!errors.isEmpty()) {
            errors.sort(null);   // stable order
            return Result.invalid(errors);
        }
        return Result.valid(request);
    }

    // e.g. "rule.severity: invalid value 'SUPER'"
    private String describeReadError(JacksonException ex) {
        String field = ex.getPath().stream()
                .map(JacksonException.Reference::getPropertyName)
                .filter(name -> name != null)
                .collect(Collectors.joining("."));

        if (field.isEmpty()) {
            return "event could not be read";
        }
        if (ex instanceof InvalidFormatException invalid) {
            return field + ": invalid value '" + invalid.getValue() + "'";
        }
        return field + ": invalid value";
    }

    public record Result(EventRequest request, List<String> errors) {

        static Result valid(EventRequest request) {
            return new Result(request, List.of());
        }

        static Result invalid(List<String> errors) {
            return new Result(null, errors);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }
    }
}
