package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.dto.whatsapp.ConnectionStatusDto;
import in.jlenterprises.ecommerce.request.whatsapp.ConnectionRequest;

/** Manage and inspect the Meta WhatsApp Cloud API connection (Connection tab). */
public interface WhatsappConnectionService {

    /** Current connection status — validates the token against Meta when one is set. */
    ConnectionStatusDto status();

    /** Persist supplied credentials (only non-blank fields) and return the fresh status. */
    ConnectionStatusDto save(ConnectionRequest request);

    /** Auto-detect the WABA id from the token, save it, and return the fresh status. */
    ConnectionStatusDto detectWaba();
}
