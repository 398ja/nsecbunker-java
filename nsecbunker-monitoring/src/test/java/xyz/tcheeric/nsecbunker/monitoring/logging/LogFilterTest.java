package xyz.tcheeric.nsecbunker.monitoring.logging;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LogFilterTest {

    @Test
    void shouldBuildWithDefaultValues() {
        LogFilter filter = LogFilter.builder().build();

        assertThat(filter.getMethods()).isEmpty();
        assertThat(filter.getUsers()).isEmpty();
        assertThat(filter.getSince()).isNull();
        assertThat(filter.getUntil()).isNull();
        assertThat(filter.getLimit()).isNull();
    }

    @Test
    void shouldBuildWithAllFields() {
        Instant since = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant until = Instant.now();
        List<String> methods = List.of("sign_event", "encrypt");
        List<String> users = List.of("npub1", "npub2");

        LogFilter filter = LogFilter.builder()
                .since(since)
                .until(until)
                .methods(methods)
                .users(users)
                .limit(100)
                .build();

        assertThat(filter.getSince()).isEqualTo(since);
        assertThat(filter.getUntil()).isEqualTo(until);
        assertThat(filter.getMethods()).containsExactly("sign_event", "encrypt");
        assertThat(filter.getUsers()).containsExactly("npub1", "npub2");
        assertThat(filter.getLimit()).isEqualTo(100);
    }

    @Test
    void hasMethodsShouldReturnTrueWhenMethodsPresent() {
        LogFilter filter = LogFilter.builder()
                .methods(List.of("sign_event"))
                .build();

        assertThat(filter.hasMethods()).isTrue();
    }

    @Test
    void hasMethodsShouldReturnFalseWhenMethodsEmpty() {
        LogFilter filter = LogFilter.builder().build();

        assertThat(filter.hasMethods()).isFalse();
    }

    @Test
    void hasMethodsShouldReturnFalseWhenMethodsNull() {
        LogFilter filter = LogFilter.builder()
                .methods(null)
                .build();

        assertThat(filter.hasMethods()).isFalse();
    }

    @Test
    void hasUsersShouldReturnTrueWhenUsersPresent() {
        LogFilter filter = LogFilter.builder()
                .users(List.of("npub1test"))
                .build();

        assertThat(filter.hasUsers()).isTrue();
    }

    @Test
    void hasUsersShouldReturnFalseWhenUsersEmpty() {
        LogFilter filter = LogFilter.builder().build();

        assertThat(filter.hasUsers()).isFalse();
    }

    @Test
    void hasUsersShouldReturnFalseWhenUsersNull() {
        LogFilter filter = LogFilter.builder()
                .users(null)
                .build();

        assertThat(filter.hasUsers()).isFalse();
    }

    @Test
    void toBuilderShouldCreateCopy() {
        LogFilter original = LogFilter.builder()
                .methods(List.of("sign_event"))
                .limit(50)
                .build();

        LogFilter copy = original.toBuilder()
                .limit(100)
                .build();

        assertThat(copy.getMethods()).containsExactly("sign_event");
        assertThat(copy.getLimit()).isEqualTo(100);
        assertThat(original.getLimit()).isEqualTo(50);
    }

    @Test
    void equalsShouldWorkCorrectly() {
        LogFilter filter1 = LogFilter.builder()
                .methods(List.of("sign_event"))
                .limit(100)
                .build();

        LogFilter filter2 = LogFilter.builder()
                .methods(List.of("sign_event"))
                .limit(100)
                .build();

        assertThat(filter1).isEqualTo(filter2);
        assertThat(filter1.hashCode()).isEqualTo(filter2.hashCode());
    }
}
