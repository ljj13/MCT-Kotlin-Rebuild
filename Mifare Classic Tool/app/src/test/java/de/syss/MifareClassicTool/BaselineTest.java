package de.syss.MifareClassicTool;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BaselineTest {

    @Test
    public void testHexAndBytesConversion() {
        byte[] expectedBytes = new byte[]{(byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF};
        String hexString = "0123456789ABCDEF";

        byte[] convertedBytes = Common.hex2Bytes(hexString);
        assertArrayEquals(expectedBytes, convertedBytes);

        String convertedHex = Common.bytes2Hex(convertedBytes);
        assertEquals(hexString, convertedHex);

        // Edge cases
        assertNull(Common.hex2Bytes(null));
        assertNull(Common.hex2Bytes("123")); // Odd length
        assertNull(Common.hex2Bytes("123G")); // Non-hex
        assertEquals("", Common.bytes2Hex(null));
        assertEquals("", Common.bytes2Hex(new byte[0]));
    }

    @Test
    public void testAsciiAndHexConversion() {
        String ascii = "Test 123!";
        String hex = Common.ascii2Hex(ascii);
        String convertedAscii = Common.hex2Ascii(hex);
        assertEquals(ascii, convertedAscii);

        assertNull(Common.ascii2Hex(null));
        assertNull(Common.ascii2Hex(""));
        assertNull(Common.hex2Ascii(null));
        assertNull(Common.hex2Ascii("ZZ"));
    }

    @Test
    public void testBccCalculation() {
        byte[] uid = new byte[]{(byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78};
        byte expectedBcc = (byte) (0x12 ^ 0x34 ^ 0x56 ^ 0x78);

        byte bcc = Common.calcBcc(uid);
        assertEquals(expectedBcc, bcc);
        assertTrue(Common.isValidBcc(uid, expectedBcc));
        assertFalse(Common.isValidBcc(uid, (byte) (expectedBcc ^ 0x01)));
    }

    @Test
    public void testIsValueBlock() {
        // Construct valid value block:
        // Value: 1, inverted: ~1, Value: 1, Addr: 5, inverted Addr: ~5, Addr: 5, inverted Addr: ~5
        // Value 1 as 4-byte LE: 01 00 00 00
        // ~Value 1: FE FF FF FF
        // Value 1: 01 00 00 00
        // Addr 5: 05
        // ~Addr 5: FA
        // Addr 5: 05
        // ~Addr 5: FA
        String validValueBlock = "01000000FEFFFFFF0100000005FA05FA";
        assertTrue(Common.isValueBlock(validValueBlock));

        // Invalid block
        String invalidValueBlock = "01000000000000000100000005FA05FA";
        assertFalse(Common.isValueBlock(invalidValueBlock));
        assertFalse(Common.isValueBlock("SHORT"));
    }

    @Test
    public void testReverseByteArrayInPlace() {
        byte[] data = new byte[]{1, 2, 3, 4};
        Common.reverseByteArrayInPlace(data);
        assertArrayEquals(new byte[]{4, 3, 2, 1}, data);

        byte[] single = new byte[]{42};
        Common.reverseByteArrayInPlace(single);
        assertArrayEquals(new byte[]{42}, single);
    }

    @Test
    public void testIsValidKeyFile() {
        assertEquals(1, Common.isValidKeyFile(null));
        assertEquals(1, Common.isValidKeyFile(new String[0]));
        assertEquals(1, Common.isValidKeyFile(new String[]{"# Only comments", "   "}));

        // Valid key
        String[] validKeys = new String[]{
                "# Header comment",
                "FFFFFFFFFFFF",
                "A0B1C2D3E4F5 # Trailing comment",
                "  a0a1a2a3a4a5  "
        };
        assertEquals(0, Common.isValidKeyFile(validKeys));

        // Invalid hex
        String[] invalidHex = new String[]{"FFFFFFFFFFFZ"};
        assertEquals(2, Common.isValidKeyFile(invalidHex));

        // Invalid length
        String[] invalidLength = new String[]{"FFFF"};
        assertEquals(3, Common.isValidKeyFile(invalidLength));
    }

    @Test
    public void testIsValidDump() {
        assertEquals(6, Common.isValidDump(null, false));
        assertEquals(6, Common.isValidDump(new String[0], false));

        // Valid dump of sector 0 (4 blocks)
        String[] validSector0 = new String[]{
                "+Sector: 0",
                "0102030405060708090A0B0C0D0E0F10",
                "00000000000000000000000000000000",
                "--------------------------------",
                "A0B1C2D3E4F5FF078069FFFFFFFFFFFF"
        };
        assertEquals(0, Common.isValidDump(validSector0, false));

        // Invalid: 5th block where a sector header is expected (returns 1)
        String[] fiveBlocks = new String[]{
                "+Sector: 0",
                "0102030405060708090A0B0C0D0E0F10",
                "00000000000000000000000000000000",
                "00000000000000000000000000000000",
                "00000000000000000000000000000000",
                "00000000000000000000000000000000" // 5th block
        };
        assertEquals(1, Common.isValidDump(fiveBlocks, false));

        // Invalid characters in block (returns 2)
        String[] invalidBlockChars = new String[]{
                "+Sector: 0",
                "0102030405060708090A0B0C0D0E0FZZ",
                "00000000000000000000000000000000",
                "00000000000000000000000000000000",
                "00000000000000000000000000000000"
        };
        assertEquals(2, Common.isValidDump(invalidBlockChars, false));

        // Invalid block length (returns 3)
        String[] invalidBlockLength = new String[]{
                "+Sector: 0",
                "0102030405060708",
                "00000000000000000000000000000000",
                "00000000000000000000000000000000",
                "00000000000000000000000000000000"
        };
        assertEquals(3, Common.isValidDump(invalidBlockLength, false));

        // Invalid: bad sector number
        String[] outOfRange = new String[]{
                "+Sector: 45",
                "0102030405060708090A0B0C0D0E0F10",
                "00000000000000000000000000000000",
                "00000000000000000000000000000000",
                "00000000000000000000000000000000"
        };
        assertEquals(4, Common.isValidDump(outOfRange, false));
    }

    @Test
    public void testHex2BinAndBin2Hex() {
        assertEquals("00001111", Common.hex2Bin("0F"));
        assertEquals("0f", Common.bin2Hex("00001111"));
        assertNull(Common.hex2Bin(null));
        assertNull(Common.bin2Hex(null));
    }
}
