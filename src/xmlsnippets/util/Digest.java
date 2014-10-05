//*******************************{begin:header}******************************//
//               fida - https://code.google.com/p/xml-snippets/
//***************************************************************************//
//
//      fida: an XML Revision Tracking and Version Control Software.
//
//      Copyright (C) 2012-2014 Jani Hautamaki <jani.hautamaki@hotmail.com>
//
//      Licensed under the terms of GNU General Public License v3.
//
//      You should have received a copy of the GNU General Public License v3
//      along with this program as the file LICENSE.txt; if not, please see
//      http://www.gnu.org/licenses/gpl-3.0.html
//
//********************************{end:header}*******************************//

package xmlsnippets.util;

// java core imports
import java.util.Arrays; // for copying arrays
// for input file reading
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
// for actual digest calculation
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Represents a digest value for a binary object (eg. a file).
 * The object contains the algorithm's name and its value.
 * There is a helper class method {@link #create(String, File)} which works
 * as factory. It faciliates the creation of {@code Digest} objects
 * corresponding to a specified {@code File}.
 *
 */
public class Digest
{

    /// MEMBER VARIABLES
    //===================

    /**
     * Digest algorithm's name
     */
    private String algo;

    /**
     * Digest value as a string of bytes
     */
    private byte[] value;


    // INTERNAL PARAMETERS
    //=====================

    /**
     * Size of the buffer used for reading file's contents.
     * Buffer is used while calculating a digest for a file.
     * Defaults to 4 kilobytes.
     */
    public static int READ_BUFFER_SIZE = 4096;

    // CONSTRUCTORS
    //==============

    /**
     * Creates an empty digest.
     */
    public Digest() {
        algo = null;
        value = null;
    } // ctor

    /**
     * Creates an initialized digest.
     *
     * @param digest_algo name of the algorithm
     * @param digest_value the digest value
     */
    public Digest(String digest_algo, byte[] digest_value) {
        this();
        set_digest(digest_algo, digest_value);
    } // ctor

    // OTHER METHODS
    //===============

    /**
     * Checks that the digest has an algorithm name and value set.
     *
     * @return {@code true} if the name and value are set. Otherwise,
     * {@code false}
     */
    public boolean is_valid() {
        return ((algo != null) && (value != null));
    } // is_valid()

    /*
     * Returns the algorithm's name.
     *
     * @return The algorithm's name, or {@code null} if no algorithm name set.
     */
    public String get_digest_algo() {
        return algo;
    } //  get_algo()

    /**
     * Returns the digest value.
     *
     * @return The digest value as an byte array, or {@code null} if no
     * digest value set.
     */
    public byte[] get_digest_value() {
        return value;
    } // get_digest()

    /**
     * Sets the digest algorithm name and value.
     *
     * @param digest_algo the digest algorithm name, must be non-{@code null}.
     * @param digest_value the digest value, must be non-{@code null},
     * and have a non-zero length.
     */
    public void set_digest(String digest_algo, byte[] digest_value) {
        if ((digest_algo == null) || (digest_value == null)) {
            throw new IllegalArgumentException(); // Programming error
        }
        if (digest_value.length == 0) {
            throw new IllegalArgumentException(); // Programming error
        }

        algo = digest_algo;
        value = Arrays.copyOf(digest_value, digest_value.length);

    } // set_digest()

    /**
     * Sets the algorithm name and digest value with a value decoded
     * from a hex string. The deserialization of the hex string is
     * done with {@link #deserialize_hex}
     * @param digest_algo the digest algorithm name
     * @param digest_hex the digest value as a hex string
     */
    public void set_hex(String digest_algo, String digest_hex) {
        if ((digest_algo == null) || (digest_hex == null)) {
            throw new IllegalArgumentException(); // Programming error
        } // if

        // Decode the hexademical string. This may throw
        value = Digest.deserialize_hex(digest_hex);

        // If the conversino succeeds, update the algorithm's name
        algo = digest_algo;
    } // set_hex()

    // FOR CONVENIENCE
    //=================

    /**
     * Returns the hex string representation of the digest value;
     * provided for convenience.
     * @return Returns {@code Digest.serialize_hex(this.value)}.
     */
    public String to_hexstring() {
        return Digest.serialize_hex(value);
    } // to_hexstring()


    // JAVA OBJECT OVERRIDES
    //=======================

    /**
     * Returns a string formatted with the algorithm's name and the digest
     * value in a hex string representation.
     */
    @Override
    public String toString() {
        if ((algo == null) || (value == null)) {
            return super.toString();
        }
        return String.format("%s:%s", algo, Digest.serialize_hex(value));
    } // toString()

    /**
     * Tests for equivalence. The digests are considered equal if and only
     * if their algorithm names and digest values match.
     * @param obj the object to compare to
     * @return {@code true} if the digest algorithms and values match.
     * Otherwise, {@code false}.
     */
    @Override
    public boolean equals(Object obj) {

        if (obj == null) {
            return false;
        } // if: not a proper object


        if ((obj instanceof Digest) == false) {
            return false;
        } // if: not an instance of this class

        // Cast
        Digest that = (Digest) obj;

        if ((that.algo == null) || (that.value == null)
            || (this.algo == null) || (this.value == null))
        {
            return false;
        } // if: uninitialized objects are never equal

        // All member variables are non-null. Those can be compared
        if (algo.equals(that.algo)
            && Arrays.equals(value, that.value))
        {
            return true;
        } // if: both member variables are equal


        return false;
    } // equals()

    /**
     * Returns a hash code corresponding to the overrided equivalence.
     * The name of the algorithm can be ignored here; if {@code equals()} is
     * returns [@code true} then the digest values must match. On the other
     * hand if {@code hashCode()}s are equal, then the digest may be equal.
     *
     * @return {@code Arrays.hashCode(digest)}
     */
    @Override
    public int hashCode() {
        if ((algo == null) || (value == null)) {
            return 0;
        }

        return Arrays.hashCode(value);
    } // hashCode()


    // CLASS METHODS: SERIALIZATIONS
    //===============================

    /**
     * Converts a byte array to a hexadecimal string represetation.
     * Each byte is converted into two ehxadecimal characters.
     * @param value the digest value
     * @return The hexadecimal string representation of the digest value.
     */
    public static String serialize_hex(byte[] value) {
       // Create empty string buffer with a proper initial capacity
        StringBuffer sb = new StringBuffer(value.length * 2);

        // Convert each byte into a 2-character hex number
        for (int i = 0; i < value.length; i++) {
            // The binary "AND" operation is there to make sure
            // the array byte value can indeed fit into two hex characters.
            // If the result is >= 0x100, the String.format() for "%02x"
            // will produce more than two characters.
            sb.append(String.format("%02x", value[i] & 0xff));
        } // for:  each byte

        return sb.toString();
    }  // serialize_hex()

    /**
     * Converts a hex representation of a value into an array of bytes.
     * The input string must have two hexadecimal characters for each byte,
     * that is, the input string's length must be even-numbered.
     *
     * @param hexstring the even-lengthed hexadecimal representation
     * of a byte array.
     *
     * @return The deserialized byte array.
     *
     * @throws NullPointerExceptino if the argument is {@code null}.
     * @throws IllegaArgumentException if the input string has an even
     * length.
     * @throws NumberFormatException if an non-hexadecimal character is
     * encountered.
     */
    public static byte[] deserialize_hex(String hexstring)
        throws NumberFormatException, IllegalArgumentException
    {
        // The length of the string
        int len = hexstring.length();

        // Count the shift operations
        int shifts = 0;

        // Value of the current byte; this is the output variable
        // of the conversions
        int cur_byte = 0;

        // Index pointing to next element of rval[] array.
        int j = 0;

        // If the string is odd-lengthed, then a leading zero is assumed.
        // The implicit leading zero is achieved by starting the shift
        // count from 1 instead of 0.
        if ((len & 1) == 1) {
            //shifts = 1;
            throw new IllegalArgumentException("odd length; even expected");
        } // if

        // Allocate the byte array that works as the return value.
        // The division is rounded upwards to make sure that odd-lengthed
        // strings have the correct number of elements in the array.
        byte[] rval = new byte[(len+1)/2];

        for (int i = 0; i < len; i++) {

            // Shift the current value by 4 bits.
            cur_byte = cur_byte << 4;

            // Consequently, increase the shift count
            shifts++;

            // Pick the current char from the string
            char c = hexstring.charAt(i);

            // Convert the hexadecimal character into an integer value.
            // "val" gets a value of -1 if "c" is not
            // a valid digit in the specified radix.
            int val = Character.digit(c, 16); // 16 = radix for hexadecimals

            // Determine the validness based on whether "val" got the value -1.
            // If it is invalid, ie. -1, throw an exception.
            if (val == -1) throw new NumberFormatException(String.format(
                "Invalid hex digit \'%c\' at offset %d in string \"%s\"", c, i, hexstring));

            // Incorporate the value to curByte
            cur_byte = cur_byte | val;

            // Handle target array writing; wite only at after each two shifts.
            if (shifts == 2) {
                // Write the current byte to the array
                rval[j] = (byte) cur_byte;
                // Increase destination index
                j++;
                // Reset curernt value and shift count
                cur_byte = 0;
                shifts = 0;
            } // if: two shifts
        } // for: each char

        // The loop should end with shifts being zero.
        assert shifts == 0;

        return rval;
    } // deserialize_hex()

    // CLASS METHODS: CALCULATION & CREATION
    //=======================================

    /**
     * Calculates the digest value of a file with the specified algorithm.
     *
     * @param algo_name the name of the algorithm
     * @param file the input file for digest calculation
     *
     * @return byte array containing the digest value
     *
     * @throws NoSuchAlgorithmException if the specified algorithm name
     * is not known.
     * @throws FileNotFoundException if the specified input file is not found
     * @throws IOException if a problem occurs with {@code File.read()}.
     */
    public static byte[] calculate(String algo_name, File file)
        throws NoSuchAlgorithmException, FileNotFoundException, IOException
    {
        // May throw if there is no such algorithm
        MessageDigest md = MessageDigest.getInstance(algo_name);

        // May throw
        FileInputStream fis = new FileInputStream(file);

        byte[] buffer = new byte[READ_BUFFER_SIZE];
        int nread = 0;

        // fis.read() may throw
        while ((nread = fis.read(buffer)) != -1) {
            md.update(buffer, 0, nread);
        } // while: not eof

        return md.digest();
    } // calculate()

    /**
     * Creates {@code Digest} objects with initial values calculated
     * from a specified file with a specified algorithm.
     * The exceptions originate from {@link #calculate(String, File)}.
     * See that for more information
     *
     * @param algo_name the digest algorithm's name
     * @param file the input file
     *
     * @return the resulting Digest object
     *
     * @throws NoSuchAlgorithmException See above.
     * @throws FileNotFoundException See above.
     * @throws IOexception See above.
     *
     */
    public static Digest create(String algo_name, File file)
        throws NoSuchAlgorithmException, FileNotFoundException, IOException
    {
        // Calculate the digest value
        byte[] value = calculate(algo_name, file);

        // Return newly created object
        return new Digest(algo_name, value);
    } // create()


} // class Digest


