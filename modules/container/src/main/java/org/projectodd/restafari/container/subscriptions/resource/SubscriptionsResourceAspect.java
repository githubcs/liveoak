package org.projectodd.restafari.container.subscriptions.resource;

import org.projectodd.restafari.container.aspects.ResourceAspect;
import org.projectodd.restafari.container.subscriptions.SubscriptionManager;
import org.projectodd.restafari.spi.resource.Resource;

/**
 * @author Bob McWhirter
 */
public class SubscriptionsResourceAspect implements ResourceAspect {

    public SubscriptionsResourceAspect(SubscriptionManager subscriptionManager) {
        this.subscriptionManager = subscriptionManager;
    }

    @Override
    public Resource forResource(Resource resource) {
        return new SubscriptionsCollectionResource( resource, this.subscriptionManager );
    }

    private SubscriptionManager subscriptionManager;
}