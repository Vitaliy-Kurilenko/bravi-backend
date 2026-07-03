package ua.com.bravi.bravi.seller.channels;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.bravi.bravi.seller.channels.api.SalesChannelsApi;
import ua.com.bravi.bravi.seller.channels.domain.SalesChannelStatus;
import ua.com.bravi.bravi.seller.channels.domain.SalesChannelType;
import ua.com.bravi.bravi.seller.channels.persistence.ISalesChannelRepository;
import ua.com.bravi.bravi.seller.channels.persistence.entity.SalesChannelEntity;
import ua.com.bravi.bravi.shared.util.PublicIdGenerator;

@Service
@RequiredArgsConstructor
public class SalesChannelService implements SalesChannelsApi {

    private static final String MANUAL_CHANNEL_NAME = "Manual Orders";

    private final ISalesChannelRepository salesChannelRepository;

    @Override
    @Transactional
    public void createManualChannel(Long storeId) {
        if (salesChannelRepository.existsByStoreIdAndType(storeId, SalesChannelType.MANUAL)) {
            return;
        }
        SalesChannelEntity channel = new SalesChannelEntity();
        channel.setPublicId(PublicIdGenerator.generate("chn"));
        channel.setStoreId(storeId);
        channel.setType(SalesChannelType.MANUAL);
        channel.setName(MANUAL_CHANNEL_NAME);
        channel.setStatus(SalesChannelStatus.ACTIVE);
        salesChannelRepository.save(channel);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasManualChannel(Long storeId) {
        return salesChannelRepository.existsByStoreIdAndType(storeId, SalesChannelType.MANUAL);
    }
}
