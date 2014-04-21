package com.github.fge.swagger.migrate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.swagger.util.SwaggerMigrators;
import com.github.fge.swagger.util.SwaggerTransformException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Objects;

/**
 * Patch a 1.1 {@code allowableValues} into a 1.2 {@code enum}
 *
 * <p>{@code allowableValues} is an object which has at least one {@code
 * valueType} member, whose value is a JSON String. If this string is {@code
 * "LIST"}, then it is replaced by an {@code enum}, as in:</p>
 *
 * <pre>
 *     {
 *         "allowableValues": {
 *             "valueType": "LIST",
 *             "values": [ "a", "b", "c" ]
 *         }
 *     }
 * </pre>
 *
 * <p>which will become:</p>
 *
 * <pre>
 *     {
 *         "enum": [ "a", "b", "c" ]
 *     }
 * </pre>
 *
 * <p>Another possible value is {@code "range[]"}, however this migrator does
 * not handle this case (yet?).</p>
 */
public final class V11AllowableValuesMigrator
    implements SwaggerMigrator
{
    private static final ObjectMapper MAPPER = JacksonUtils.newMapper();
    private static final JsonPatch PATCH;

    static {
        final String op
            = "[{"
            + "\"op\":\"move\","
            + "\"from\":\"/allowableValues/values\","
            + "\"path\":\"/enum\""
            + "},"
            + "{"
            + "\"op\": \"remove\", \"path\": \"/allowableValues\""
            + "}]";
        try {
            PATCH = MAPPER.readValue(op, JsonPatch.class);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Nonnull
    @Override
    public JsonNode migrate(@Nonnull final JsonNode input)
        throws SwaggerTransformException
    {
        Objects.requireNonNull(input);
        if (!input.has("allowableValues"))
            return input;
        final String valueType = JsonPointer.of("allowableValues", "valueType")
            .get(input).textValue();
        if (!"LIST".equals(valueType))
            throw new SwaggerTransformException();
        return SwaggerMigrators.fromPatch(PATCH).migrate(input);
    }
}
