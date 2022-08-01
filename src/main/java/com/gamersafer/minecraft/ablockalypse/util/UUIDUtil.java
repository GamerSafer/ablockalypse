package com.gamersafer.minecraft.ablockalypse.util;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UUIDUtil {

    public static final Pattern TRIMMED_UUID_PATTERN = Pattern.compile("^([a-z0-9]{8})([a-z0-9]{4})([a-z0-9]{4})([a-z0-9]{4})([a-z0-9]{12})$", Pattern.CASE_INSENSITIVE);

    /**
     * Parses a trimmed {@link UUID}.
     *
     * @param string the {@link String} to parse into a {@link UUID}
     * @return , otherwise an {@link Optional} containing the
     * parsed {@link UUID}, othwerwise a {@link Optional#empty()} if the passed {@link String} is not a
     * valid trimmed UUID
     */
    public static Optional<UUID> parseUUID(String string) {
        String result = null;

        Matcher matcher = TRIMMED_UUID_PATTERN.matcher(string);
        if (matcher.matches()) {
            StringBuilder sb = new StringBuilder();

            for (int i = 1; i <= matcher.groupCount(); i++) {
                if (i > 1) {
                    sb.append("-");
                }
                sb.append(matcher.group(i));
            }

            result = sb.toString();
        }

        if (result != null) {
            return Optional.of(UUID.fromString(result));
        }
        return Optional.empty();
    }

}
