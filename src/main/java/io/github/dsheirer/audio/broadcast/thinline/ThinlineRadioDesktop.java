/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.audio.broadcast.thinline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.dsheirer.audio.playback.AudioOutput;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;
import java.util.Iterator;
import java.util.Base64;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Streams the screen SDRTrunk is showing and applies mouse and keyboard input on that screen.
 */
class ThinlineRadioDesktop
{
    private static final Logger mLog = LoggerFactory.getLogger(ThinlineRadioDesktop.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_WIDTH = 3840;
    private static final long FRAME_MS = 200;

    private final JFrame mFrame;
    private final Predicate<ObjectNode> mSend;
    private final Consumer<ObjectNode> mQueue;
    private final BooleanSupplier mReady;
    private final Object mRobotLock = new Object();
    private final Object mAudioLock = new Object();
    private final ByteArrayOutputStream mAudio = new ByteArrayOutputStream();

    private volatile boolean mEnabled;
    private Thread mThread;
    private Robot mRobot;
    private int mOriginX;
    private int mOriginY;
    private int mScreenWidth = 1;
    private int mScreenHeight = 1;
    private int mAudioChannels = 1;
    private int mAudioRate = 8000;

    ThinlineRadioDesktop(JFrame frame, Predicate<ObjectNode> send, Consumer<ObjectNode> queue, BooleanSupplier ready)
    {
        mFrame = frame;
        mSend = send;
        mQueue = queue;
        mReady = ready;
    }

    void setEnabled(boolean enabled)
    {
        if(enabled)
        {
            start();
            return;
        }
        stopLoop();
    }

    void input(JsonNode command)
    {
        if(!mEnabled)
        {
            return;
        }
        String kind = command.path("kind").asText("");
        try
        {
            synchronized(mRobotLock)
            {
                if(mRobot == null)
                {
                    mRobot = new Robot();
                    mRobot.setAutoDelay(0);
                }
                switch(kind)
                {
                    case "move", "down", "up" -> pointer(command, kind);
                    case "wheel" ->
                    {
                        move(command);
                        int clicks = (int)Math.signum(command.path("delta").asDouble(0));
                        if(clicks != 0)
                        {
                            mRobot.mouseWheel(clicks);
                        }
                    }
                    case "key" -> key(command);
                    default ->
                    {
                    }
                }
            }
        }
        catch(Exception e)
        {
            mLog.debug("Desktop input failed", e);
        }
    }

    private void start()
    {
        if(mEnabled)
        {
            return;
        }
        mEnabled = true;
        AudioOutput.setRemoteListener(this::onPlayback);
        raiseFrame();
        mThread = new Thread(this::loop, "thinline-desktop");
        mThread.setDaemon(true);
        mThread.start();
    }

    private void stopLoop()
    {
        mEnabled = false;
        AudioOutput.setRemoteListener(null);
        synchronized(mAudioLock)
        {
            mAudio.reset();
        }
        Thread thread = mThread;
        mThread = null;
        if(thread != null)
        {
            thread.interrupt();
        }
    }

    private void raiseFrame()
    {
        if(mFrame == null)
        {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            if((mFrame.getExtendedState() & Frame.ICONIFIED) == Frame.ICONIFIED)
            {
                mFrame.setExtendedState(Frame.NORMAL);
            }
            mFrame.setVisible(true);
            mFrame.toFront();
        });
    }

    private void loop()
    {
        while(mEnabled)
        {
            long started = System.currentTimeMillis();
            try
            {
                if(mReady.getAsBoolean())
                {
                    capture();
                    sendAudio();
                }
            }
            catch(Exception e)
            {
                mLog.debug("Desktop capture failed", e);
            }
            long pause = FRAME_MS - (System.currentTimeMillis() - started);
            if(pause > 0)
            {
                try
                {
                    Thread.sleep(pause);
                }
                catch(InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void capture() throws Exception
    {
        Rectangle bounds = screenBounds();
        if(bounds.width < 2 || bounds.height < 2)
        {
            return;
        }
        BufferedImage shot;
        synchronized(mRobotLock)
        {
            if(mRobot == null)
            {
                mRobot = new Robot();
                mRobot.setAutoDelay(0);
            }
            shot = mRobot.createScreenCapture(bounds);
        }
        mOriginX = bounds.x;
        mOriginY = bounds.y;
        mScreenWidth = bounds.width;
        mScreenHeight = bounds.height;

        int targetWidth = Math.min(MAX_WIDTH, shot.getWidth());
        int targetHeight = Math.max(1, shot.getHeight() * targetWidth / shot.getWidth());
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.drawImage(shot, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();

        byte[] jpeg = writeJpeg(scaled);
        ObjectNode body = MAPPER.createObjectNode();
        body.put("type", "desktop");
        body.put("originX", mOriginX);
        body.put("originY", mOriginY);
        body.put("screenWidth", mScreenWidth);
        body.put("screenHeight", mScreenHeight);
        body.put("image", Base64.getEncoder().encodeToString(jpeg));
        mSend.test(body);
    }

    private void onPlayback(byte[] pcm, int channels, int sampleRate)
    {
        if(!mEnabled || pcm == null || pcm.length == 0)
        {
            return;
        }
        synchronized(mAudioLock)
        {
            if(mAudio.size() > 64000)
            {
                mAudio.reset();
            }
            mAudio.write(pcm, 0, pcm.length);
            mAudioChannels = Math.max(1, channels);
            mAudioRate = sampleRate > 0 ? sampleRate : 8000;
        }
    }

    private void sendAudio()
    {
        byte[] pcm;
        int channels;
        int rate;
        synchronized(mAudioLock)
        {
            if(mAudio.size() < 2)
            {
                return;
            }
            pcm = mAudio.toByteArray();
            mAudio.reset();
            channels = mAudioChannels;
            rate = mAudioRate;
        }
        ObjectNode body = MAPPER.createObjectNode();
        body.put("type", "audio");
        body.put("rate", rate);
        body.put("channels", channels);
        body.put("pcm", Base64.getEncoder().encodeToString(pcm));
        mQueue.accept(body);
    }

    private Rectangle screenBounds()
    {
        if(mFrame != null)
        {
            GraphicsConfiguration configuration = mFrame.getGraphicsConfiguration();
            if(configuration != null)
            {
                return configuration.getBounds();
            }
        }
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();
    }

    private byte[] writeJpeg(BufferedImage image) throws Exception
    {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if(!writers.hasNext())
        {
            throw new IllegalStateException("No JPEG writer");
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writer.setOutput(new MemoryCacheImageOutputStream(output));
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.92f);
        writer.write(null, new IIOImage(image, null, null), param);
        writer.dispose();
        return output.toByteArray();
    }

    private void pointer(JsonNode command, String kind)
    {
        move(command);
        if("move".equals(kind))
        {
            return;
        }
        int button = command.path("button").asInt(0);
        int mask = switch(button)
        {
            case 1 -> InputEvent.BUTTON2_DOWN_MASK;
            case 2 -> InputEvent.BUTTON3_DOWN_MASK;
            default -> InputEvent.BUTTON1_DOWN_MASK;
        };
        if("down".equals(kind))
        {
            mRobot.mousePress(mask);
        }
        else
        {
            mRobot.mouseRelease(mask);
        }
    }

    private void move(JsonNode command)
    {
        int x = mOriginX + (int)Math.round(command.path("x").asDouble(0));
        int y = mOriginY + (int)Math.round(command.path("y").asDouble(0));
        x = Math.max(mOriginX, Math.min(mOriginX + mScreenWidth - 1, x));
        y = Math.max(mOriginY, Math.min(mOriginY + mScreenHeight - 1, y));
        mRobot.mouseMove(x, y);
    }

    private void key(JsonNode command)
    {
        int code = keyCode(command.path("code").asText(""));
        if(code == 0)
        {
            return;
        }
        if(command.path("down").asBoolean())
        {
            mRobot.keyPress(code);
        }
        else
        {
            mRobot.keyRelease(code);
        }
    }

    private int keyCode(String code)
    {
        if(code == null || code.isBlank())
        {
            return 0;
        }
        if(code.startsWith("Key") && code.length() == 4)
        {
            return KeyEvent.VK_A + (code.charAt(3) - 'A');
        }
        if(code.startsWith("Digit") && code.length() == 6)
        {
            return KeyEvent.VK_0 + (code.charAt(5) - '0');
        }
        if(code.startsWith("Numpad") && code.length() == 7 && Character.isDigit(code.charAt(6)))
        {
            return KeyEvent.VK_NUMPAD0 + (code.charAt(6) - '0');
        }
        if(code.startsWith("F") && code.length() <= 3)
        {
            try
            {
                int number = Integer.parseInt(code.substring(1));
                if(number >= 1 && number <= 12)
                {
                    return KeyEvent.VK_F1 + number - 1;
                }
            }
            catch(NumberFormatException ignored)
            {
            }
        }
        return switch(code)
        {
            case "Enter", "NumpadEnter" -> KeyEvent.VK_ENTER;
            case "Escape" -> KeyEvent.VK_ESCAPE;
            case "Tab" -> KeyEvent.VK_TAB;
            case "Backspace" -> KeyEvent.VK_BACK_SPACE;
            case "Delete" -> KeyEvent.VK_DELETE;
            case "Insert" -> KeyEvent.VK_INSERT;
            case "Home" -> KeyEvent.VK_HOME;
            case "End" -> KeyEvent.VK_END;
            case "PageUp" -> KeyEvent.VK_PAGE_UP;
            case "PageDown" -> KeyEvent.VK_PAGE_DOWN;
            case "ArrowLeft" -> KeyEvent.VK_LEFT;
            case "ArrowRight" -> KeyEvent.VK_RIGHT;
            case "ArrowUp" -> KeyEvent.VK_UP;
            case "ArrowDown" -> KeyEvent.VK_DOWN;
            case "Space" -> KeyEvent.VK_SPACE;
            case "ShiftLeft", "ShiftRight" -> KeyEvent.VK_SHIFT;
            case "ControlLeft", "ControlRight" -> KeyEvent.VK_CONTROL;
            case "AltLeft", "AltRight" -> KeyEvent.VK_ALT;
            case "MetaLeft", "MetaRight" -> KeyEvent.VK_WINDOWS;
            case "Minus" -> KeyEvent.VK_MINUS;
            case "Equal" -> KeyEvent.VK_EQUALS;
            case "Comma" -> KeyEvent.VK_COMMA;
            case "Period" -> KeyEvent.VK_PERIOD;
            case "Slash" -> KeyEvent.VK_SLASH;
            case "Backslash" -> KeyEvent.VK_BACK_SLASH;
            case "BracketLeft" -> KeyEvent.VK_OPEN_BRACKET;
            case "BracketRight" -> KeyEvent.VK_CLOSE_BRACKET;
            case "Semicolon" -> KeyEvent.VK_SEMICOLON;
            case "Quote" -> KeyEvent.VK_QUOTE;
            case "Backquote" -> KeyEvent.VK_BACK_QUOTE;
            default -> 0;
        };
    }
}
