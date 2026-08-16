package com.eshop.app.user.shared.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;

import com.eshop.app.user.domain.entity.User;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

/**
 * Regression guard for the persistence-safety invariant documented on {@link UserRole}'s
 * Javadoc: {@code User.role} must stay persisted by name, not ordinal, or a future reordering
 * of this enum's constants would silently remap every stored role to a different meaning.
 *
 * <p>Previously, this class also guarded parity between {@code UserRole} and a separate
 * {@code com.eshop.app.user.domain.entity.Role} JPA-persisted enum with identical constant
 * names. That duplication has since been removed — {@code User.role} is now typed directly as
 * {@link UserRole}, so there is nothing left to keep in sync.
 */
class UserRoleTest {

    @Test
    void userEntity_persistsRoleByNameNotOrdinal() throws NoSuchFieldException {
        Field roleField = User.class.getDeclaredField("role");
        Enumerated enumerated = roleField.getAnnotation(Enumerated.class);

        assertThat(enumerated)
                .as("User.role must stay annotated @Enumerated — its absence defaults to "
                        + "EnumType.ORDINAL, which silently corrupts every stored role if this "
                        + "enum's declaration order is ever changed")
                .isNotNull();
        assertThat(enumerated.value())
                .as("User.role must be persisted as EnumType.STRING, not ORDINAL — ordinal "
                        + "persistence silently remaps every stored role to a different meaning "
                        + "if a constant is ever inserted, removed, or reordered")
                .isEqualTo(EnumType.STRING);
        assertThat(roleField.getType())
                .as("User.role must be UserRole directly — not a separate duplicate enum")
                .isEqualTo(UserRole.class);
    }
}
