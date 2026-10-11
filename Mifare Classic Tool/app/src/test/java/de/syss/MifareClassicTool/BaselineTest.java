package de.syss.MifareClassicTool;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.syss.MifareClassicTool.Activities.CloneUidTool;

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

    @Test
    public void testAccessConditionsMatrixAndRoundTrip() {
        // Transport configuration AC bytes: FF 07 80
        byte[] transportAc = new byte[]{(byte) 0xFF, (byte) 0x07, (byte) 0x80};
        byte[][] matrix = Common.acBytesToACMatrix(transportAc);
        // Should decode into 3 rows (C1, C2, C3) x 4 blocks
        assertEquals(3, matrix.length);
        assertEquals(4, matrix[0].length);

        // Blocks 0..2 should have C1=0, C2=0, C3=0
        for (int block = 0; block < 3; block++) {
            assertEquals(0, matrix[0][block]);
            assertEquals(0, matrix[1][block]);
            assertEquals(0, matrix[2][block]);
        }
        // Sector trailer (block 3) should have C1=0, C2=0, C3=1
        assertEquals(0, matrix[0][3]);
        assertEquals(0, matrix[1][3]);
        assertEquals(1, matrix[2][3]);

        // Round-trip encoding
        byte[] encodedAc = Common.acMatrixToACBytes(matrix);
        assertArrayEquals(transportAc, encodedAc);

        // Invalid AC bytes (inverted bits do not match)
        byte[] invalidAc = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00};
        assertNull(Common.acBytesToACMatrix(invalidAc));

        // Invalid inputs
        assertNull(Common.acBytesToACMatrix(null));
        assertNull(Common.acBytesToACMatrix(new byte[]{1, 2}));
        assertNull(Common.acMatrixToACBytes(null));
    }

    @Test
    public void testIsKeyBReadable() {
        assertTrue(Common.isKeyBReadable((byte) 0, (byte) 0, (byte) 0));
        assertTrue(Common.isKeyBReadable((byte) 0, (byte) 1, (byte) 0));
        assertTrue(Common.isKeyBReadable((byte) 0, (byte) 0, (byte) 1));

        assertFalse(Common.isKeyBReadable((byte) 0, (byte) 1, (byte) 1));
        assertFalse(Common.isKeyBReadable((byte) 1, (byte) 0, (byte) 0));
        assertFalse(Common.isKeyBReadable((byte) 1, (byte) 1, (byte) 0));
        assertFalse(Common.isKeyBReadable((byte) 1, (byte) 0, (byte) 1));
        assertFalse(Common.isKeyBReadable((byte) 1, (byte) 1, (byte) 1));
    }

    @Test
    public void testOperationRequirements() {
        // Data block with C1=0, C2=0, C3=0 and Key B readable
        // Read: returns 1 (Key A) when Key B is readable, or 3 (Key A|B) if not
        assertEquals(1, Common.getOperationRequirements((byte) 0, (byte) 0, (byte) 0,
                Common.Operation.Read, false, true));
        assertEquals(3, Common.getOperationRequirements((byte) 0, (byte) 0, (byte) 0,
                Common.Operation.Read, false, false));

        // Sector trailer with C1=0, C2=0, C3=1
        // Read Key A: returns 0 (Never)
        assertEquals(0, Common.getOperationRequirements((byte) 0, (byte) 0, (byte) 1,
                Common.Operation.ReadKeyA, true, false));
        // Read AC: returns 1 (Key A)
        assertEquals(1, Common.getOperationRequirements((byte) 0, (byte) 0, (byte) 1,
                Common.Operation.ReadAC, true, false));
    }

    @Test
    public void testDecimalAndHexConversionBigInteger() {
        // Large decimal requiring BigInteger (> 18 digits)
        String largeDec = "12345678901234567890";
        String hexFromDec = Common.decBE2Hex(largeDec);
        String decFromHex = Common.hex2DecBE(hexFromDec);
        assertEquals(largeDec, decFromHex);

        // Standard decimal
        assertEquals("0A", Common.decBE2Hex("10"));
        assertEquals("10", Common.hex2DecBE("0A"));

        // Little Endian conversion
        String leHex = Common.decLE2Hex("256"); // 256 in hex is 0100, LE is 0001
        assertEquals("0001", leHex);
        assertEquals("256", Common.hex2DecLE(leHex));

        assertNull(Common.decBE2Hex("not_a_number"));
        assertNull(Common.decLE2Hex(null));
        assertNull(Common.hex2DecBE("invalid_hex"));
        assertNull(Common.hex2DecLE(null));
    }

    @Test
    public void testByte2FmtString() {
        byte[] bytes = new byte[]{(byte) 0x12, (byte) 0x34};
        // Format 0 = Hex
        assertEquals("1234", Common.byte2FmtString(bytes, 0));
        // Format 1 = DecBE (0x1234 = 4660)
        assertEquals("4660", Common.byte2FmtString(bytes, 1));
        // Format 2 = DecLE (0x3412 = 13330)
        assertEquals("13330", Common.byte2FmtString(bytes, 2));
    }

    @Test
    public void testBlockToSector() {
        // Standard 1K/2K sectors (4 blocks each, sectors 0..31)
        assertEquals(0, MCReader.blockToSector(0));
        assertEquals(0, MCReader.blockToSector(3));
        assertEquals(1, MCReader.blockToSector(4));
        assertEquals(31, MCReader.blockToSector(127));

        // 4K large sectors (16 blocks each, sectors 32..39)
        assertEquals(32, MCReader.blockToSector(128));
        assertEquals(32, MCReader.blockToSector(143));
        assertEquals(33, MCReader.blockToSector(144));
        assertEquals(39, MCReader.blockToSector(255));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testBlockToSectorNegativeThrows() {
        MCReader.blockToSector(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testBlockToSectorOver255Throws() {
        MCReader.blockToSector(256);
    }

    @Test
    public void testIsValidBlock0() {
        // Valid 4-byte UID block 0:
        // UID: 01020304, BCC: 04, SAK: 08 (MFC 1K), ATQA: 0400
        // SAK start for 4-byte UID = 4*2 + 2 = 10 -> substring(10, 12) = 08
        // ATQA start = 12 -> substring(12, 16) = 0400
        // Rest = 16 bytes total (32 hex chars)
        String validBlock0 = "01020304040804000000000000000000";
        // Check BCC = 0x01 ^ 0x02 ^ 0x03 ^ 0x04 = 0x04
        assertTrue(Common.isValidBlock0(validBlock0, 4, 1024, false));

        // Invalid BCC
        String badBccBlock0 = "01020304FF0804000000000000000000";
        assertFalse(Common.isValidBlock0(badBccBlock0, 4, 1024, false));

        // Skip BCC check should pass
        assertTrue(Common.isValidBlock0(badBccBlock0, 4, 1024, true));

        // Forbidden initial byte 0x88 or 0xF8 for 4-byte UID
        String forbidden88 = "880203048D0804000000000000000000";
        assertFalse(Common.isValidBlock0(forbidden88, 4, 1024, true));

        // Invalid length
        assertFalse(Common.isValidBlock0("SHORT", 4, 1024, true));
        assertFalse(Common.isValidBlock0(null, 4, 1024, true));
    }

    @Test
    public void testCloneUidCalcSakAtqa() {
        // 4-byte UID for 1K (1024 bytes): SAK=08, ATQA=0400 -> 080400
        assertEquals("080400", CloneUidTool.calcSakAtqa(4, 1024));

        // 4-byte UID for 2K (2048 bytes): SAK=08, ATQA=0400 -> 080400
        assertEquals("080400", CloneUidTool.calcSakAtqa(4, 2048));

        // 4-byte UID for 4K (4096 bytes): SAK=18, ATQA=0200 -> 180200
        assertEquals("180200", CloneUidTool.calcSakAtqa(4, 4096));

        // 4-byte UID for Mini (320 bytes): SAK=09, ATQA=0400 -> 090400
        assertEquals("090400", CloneUidTool.calcSakAtqa(4, 320));

        // 7-byte UID for 1K: SAK=08, ATQA=4400 -> 084400
        assertEquals("084400", CloneUidTool.calcSakAtqa(7, 1024));

        // 7-byte UID for 4K: SAK=18, ATQA=4200 -> 184200
        assertEquals("184200", CloneUidTool.calcSakAtqa(7, 4096));

        // Unsupported UID length or tag size returns null
        assertNull(CloneUidTool.calcSakAtqa(10, 1024));
        assertNull(CloneUidTool.calcSakAtqa(4, 512));
    }
}
