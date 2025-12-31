package xyz.tcheeric.nsecbunker.protocol.nip46;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Nip46Response}.
 */
class Nip46ResponseTest {

    private static final String TEST_ID = "test-id";

    @Test
    void builderCreatesValidSuccessResponse() {
        Nip46Response response = Nip46Response.builder()
                .id(TEST_ID)
                .result("success")
                .build();

        assertEquals(TEST_ID, response.getId());
        assertEquals("success", response.getResult());
        assertNull(response.getError());
    }

    @Test
    void builderCreatesValidErrorResponse() {
        Nip46Error error = Nip46Error.of("ERROR", "Something went wrong");
        Nip46Response response = Nip46Response.builder()
                .id(TEST_ID)
                .error(error)
                .build();

        assertEquals(TEST_ID, response.getId());
        assertNull(response.getResult());
        assertEquals(error, response.getError());
    }

    @Test
    void isSuccessReturnsTrueForSuccessResponse() {
        Nip46Response response = Nip46Response.success(TEST_ID, "result");

        assertTrue(response.isSuccess());
        assertFalse(response.isError());
    }

    @Test
    void isErrorReturnsTrueForErrorResponse() {
        Nip46Response response = Nip46Response.error(TEST_ID, "CODE", "message");

        assertTrue(response.isError());
        assertFalse(response.isSuccess());
    }

    @Test
    void getResultOrThrowReturnsResultForSuccess() {
        Nip46Response response = Nip46Response.success(TEST_ID, "result");

        assertEquals("result", response.getResultOrThrow());
    }

    @Test
    void getResultOrThrowThrowsForError() {
        Nip46Response response = Nip46Response.error(TEST_ID, "CODE", "message");

        assertThrows(IllegalStateException.class, response::getResultOrThrow);
    }

    @Test
    void getErrorCodeReturnsCodeForError() {
        Nip46Response response = Nip46Response.error(TEST_ID, "ERROR_CODE", "message");

        assertEquals("ERROR_CODE", response.getErrorCode());
    }

    @Test
    void getErrorCodeReturnsNullForSuccess() {
        Nip46Response response = Nip46Response.success(TEST_ID, "result");

        assertNull(response.getErrorCode());
    }

    @Test
    void getErrorMessageReturnsMessageForError() {
        Nip46Response response = Nip46Response.error(TEST_ID, "CODE", "error message");

        assertEquals("error message", response.getErrorMessage());
    }

    @Test
    void getErrorMessageReturnsNullForSuccess() {
        Nip46Response response = Nip46Response.success(TEST_ID, "result");

        assertNull(response.getErrorMessage());
    }

    // Factory method tests

    @Test
    void successCreatesValidResponse() {
        Nip46Response response = Nip46Response.success(TEST_ID, "result");

        assertEquals(TEST_ID, response.getId());
        assertEquals("result", response.getResult());
        assertTrue(response.isSuccess());
    }

    @Test
    void errorWithNip46ErrorCreatesValidResponse() {
        Nip46Error error = Nip46Error.unauthorized("Permission denied");
        Nip46Response response = Nip46Response.error(TEST_ID, error);

        assertEquals(TEST_ID, response.getId());
        assertEquals(error, response.getError());
        assertTrue(response.isError());
    }

    @Test
    void errorWithCodeAndMessageCreatesValidResponse() {
        Nip46Response response = Nip46Response.error(TEST_ID, "CODE", "message");

        assertEquals(TEST_ID, response.getId());
        assertEquals("CODE", response.getErrorCode());
        assertEquals("message", response.getErrorMessage());
    }

    @Test
    void ackCreatesAckResponse() {
        Nip46Response response = Nip46Response.ack(TEST_ID);

        assertEquals(TEST_ID, response.getId());
        assertEquals("ack", response.getResult());
        assertTrue(response.isSuccess());
    }

    @Test
    void pongCreatesPongResponse() {
        Nip46Response response = Nip46Response.pong(TEST_ID);

        assertEquals(TEST_ID, response.getId());
        assertEquals("pong", response.getResult());
        assertTrue(response.isSuccess());
    }

    @Test
    void equalsAndHashCode() {
        Nip46Response response1 = Nip46Response.success(TEST_ID, "result");
        Nip46Response response2 = Nip46Response.success(TEST_ID, "result");

        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
    }

    @Test
    void toStringForSuccessContainsResult() {
        Nip46Response response = Nip46Response.success(TEST_ID, "result");

        String str = response.toString();
        assertTrue(str.contains(TEST_ID));
        assertTrue(str.contains("result"));
    }

    @Test
    void toStringForErrorContainsError() {
        Nip46Response response = Nip46Response.error(TEST_ID, "CODE", "message");

        String str = response.toString();
        assertTrue(str.contains(TEST_ID));
        assertTrue(str.contains("error"));
    }
}
