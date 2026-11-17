package com.dtca.busvalidator.busvalidatorsdk.model;


import com.dtca.busvalidator.busvalidatorsdk.FelicaCard;

/**
 * Interface defining read and write operations for FeliCa card data.
 * <p>
 * This interface provides the contract for interacting with physical FeliCa cards,
 * allowing implementations to read card data and write information back to the card.
 * Implementations must handle low-level FeliCa communication protocols.
 * </p>
 * 
 * <h3>Operations:</h3>
 * <ul>
 *   <li><b>Read:</b> Retrieve data from the card</li>
 *   <li><b>Write:</b> Write data to specific service blocks on the card</li>
 * </ul>
 * 
 * <h3>Implementation Notes:</h3>
 * <p>
 * Implementations should handle NFC communication, error recovery,
 * and FeliCa-specific protocols. All operations may throw exceptions
 * if card communication fails.
 * </p>
 * 
 * @author DTCA Bus Validator SDK
 * @version 1.0
 * @since 2025-08-18
 * @see FelicaCard
 */
public interface ReadWriteInCard {
    /**
     * Reads data from the FeliCa card.
     * <p>
     * This method communicates with the physical card via NFC to retrieve
     * all accessible data blocks and constructs a FelicaCard object containing
     * the card information.
     * </p>
     * 
     * @return a FelicaCard object containing all data read from the card
     * @throws Exception if card communication fails, card is not present,
     *                   or data cannot be read
     */
    FelicaCard readData() throws Exception;

    /**
     * Writes data to specific blocks on the FeliCa card.
     * <p>
     * This method writes binary data to specified service and block locations
     * on the physical card. The operation requires proper authentication and
     * may fail if the card is write-protected or communication is interrupted.
     * </p>
     *
     * @param serviceNum the number of services to write to
     * @param serviceList byte array containing service codes to write
     * @param blockNum the number of blocks to write
     * @param blockList byte array containing block numbers to write
     * @param blockData byte array containing the actual data to write to the blocks
     * @return status code indicating write success or failure
     * @throws Exception if card communication fails, authentication fails,
     *                   card is write-protected, or data cannot be written
     */
    int writeInCard(int serviceNum, byte[] serviceList, int blockNum, byte[] blockList, byte[] blockData) throws Exception;
}