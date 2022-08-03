package io.github.dsheirer.dsp.symbol;

import io.github.dsheirer.sample.CapturingListener;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;

class BinaryToByteBufferAssemblerTest {

  @Test
  void shouldCallListenerOnFullBuffer() {
    // given
    final var listener = new CapturingListener<ByteBuffer>();
    final var assembler = new BinaryToByteBufferAssembler(1);
    assembler.setBufferListener(listener);

    // when
    for (var i = 0; i < 8; i++) assembler.process(true);

    // then
    assertThat(listener.capturedValues()).hasSize(1);

    final var buffer = listener.capturedValues().get(0);
    final var softly = new SoftAssertions();
    softly.assertThat(buffer.limit()).isEqualTo(1);
    softly.assertThat(buffer.position()).isEqualTo(0);
    softly.assertThat(buffer.get(0)).isEqualTo((byte) 0b11111111);
    softly.assertAll();
  }

  @Test
  void shouldResetBufferWhenFull() {
    // given
    final var listener = new CapturingListener<ByteBuffer>();
    final var assembler = new BinaryToByteBufferAssembler(1);
    assembler.setBufferListener(listener);

    // when
    for (var i = 0; i < 8; i++) assembler.process(true);
    for (var i = 0; i < 8; i++) assembler.process(false);

    // then
    assertThat(listener.capturedValues()).hasSize(2);

    final var buffer = listener.capturedValues().get(1);
    final var softly = new SoftAssertions();
    softly.assertThat(buffer.limit()).isEqualTo(1);
    softly.assertThat(buffer.position()).isEqualTo(0);
    softly.assertThat(buffer.get(0)).isEqualTo((byte) 0b00000000);
    softly.assertAll();
  }
}
