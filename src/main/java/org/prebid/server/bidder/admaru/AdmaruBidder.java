package org.prebid.server.bidder.admaru;

import com.fasterxml.jackson.core.type.TypeReference;
import com.iab.openrtb.request.BidRequest;
import com.iab.openrtb.request.Imp;
import com.iab.openrtb.response.Bid;
import com.iab.openrtb.response.BidResponse;
import com.iab.openrtb.response.SeatBid;
import io.vertx.core.http.HttpMethod;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.prebid.server.bidder.Bidder;
import org.prebid.server.bidder.model.*;
import org.prebid.server.exception.PreBidException;
import org.prebid.server.json.DecodeException;
import org.prebid.server.json.JacksonMapper;
import org.prebid.server.proto.openrtb.ext.ExtPrebid;
import org.prebid.server.proto.openrtb.ext.request.admaru.ExtImpAdmaru;
import org.prebid.server.proto.openrtb.ext.response.BidType;
import org.prebid.server.util.HttpUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Admaru {@link Bidder} implementation.
 */
public class AdmaruBidder implements Bidder<BidRequest> {

    private static final TypeReference<ExtPrebid<?, ExtImpAdmaru>> ADMARU_EXT_TYPE_REFERENCE =
            new TypeReference<>() {
            };

    private final String endpointUrl;
    private final JacksonMapper mapper;

    public AdmaruBidder(String endpointUrl, JacksonMapper mapper) {
        this.endpointUrl = HttpUtil.validateUrl(Objects.requireNonNull(endpointUrl));
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Result<List<HttpRequest<BidRequest>>> makeHttpRequests(BidRequest request) {
        final List<BidderError> errors = new ArrayList<>();

        if (CollectionUtils.isEmpty(request.getImp())) {
            return Result.withError(BidderError.badInput("No valid impressions in the bid request"));
        }

        String pubid = null;
        try {
            final ExtImpAdmaru extImpAdmaru = parseImpExt(request.getImp().get(0));
            final String adspaceid = extImpAdmaru.getAdspaceid();
            pubid = extImpAdmaru.getPubid();

            if (StringUtils.isEmpty(pubid) || StringUtils.isEmpty(adspaceid)) {
                errors.add(BidderError.badInput("No pubid or adspaceid in the bid request"));
                return Result.withErrors(errors);
            }
        } catch (PreBidException e) {
            errors.add(BidderError.badInput(e.getMessage()));
        }

        //final String requestUrl = "%s/%s/request".formatted(endpointUrl, HttpUtil.encodeUrl(partnerId));
        final String requestUrl = "%s/%s/request".formatted(endpointUrl, pubid);

        return Result.of(Collections.singletonList(
                        HttpRequest.<BidRequest>builder()
                                .method(HttpMethod.POST)
                                .uri(requestUrl)
                                .body(mapper.encodeToBytes(request))
                                .headers(HttpUtil.headers())
                                .payload(request)
                                .build()),
                errors);
    }

    private ExtImpAdmaru parseImpExt(Imp imp) {
        try {
            return mapper.mapper().convertValue(imp.getExt(), ADMARU_EXT_TYPE_REFERENCE).getBidder();
        } catch (IllegalArgumentException e) {
            throw new PreBidException(e.getMessage(), e);
        }
    }

    @Override
    public Result<List<BidderBid>> makeBids(BidderCall<BidRequest> httpCall, BidRequest bidRequest) {
        final BidResponse bidResponse;
        try {
            bidResponse = decodeBodyToBidResponse(httpCall);
        } catch (PreBidException e) {
            return Result.withError(BidderError.badInput(e.getMessage()));
        }

        final List<BidderBid> bidderBids = new ArrayList<>();
        for (SeatBid seatBid : bidResponse.getSeatbid()) {
            for (Bid bid : seatBid.getBid()) {
                final BidType bidType = getBidType(bid.getImpid(), bidRequest.getImp());
                if (bidType == BidType.banner || bidType == BidType.video) {
                    final BidderBid bidderBid = BidderBid.of(bid, bidType, bidResponse.getCur());
                    bidderBids.add(bidderBid);
                }
            }
        }
        return Result.withValues(bidderBids);
    }

    private BidResponse decodeBodyToBidResponse(BidderCall<BidRequest> httpCall) {
        try {
            return mapper.decodeValue(httpCall.getResponse().getBody(), BidResponse.class);
        } catch (DecodeException e) {
            throw new PreBidException(e.getMessage(), e);
        }
    }

    private static BidType getBidType(String impId, List<Imp> imps) {
        for (Imp imp : imps) {
            if (imp.getId().equals(impId)) {
                if (imp.getBanner() != null) {
                    return BidType.banner;
                } else if (imp.getVideo() != null) {
                    return BidType.video;
                } else if (imp.getXNative() != null) {
                    return BidType.xNative;
                } else if (imp.getAudio() != null) {
                    return BidType.audio;
                }
            }
        }
        return BidType.banner;
    }
}