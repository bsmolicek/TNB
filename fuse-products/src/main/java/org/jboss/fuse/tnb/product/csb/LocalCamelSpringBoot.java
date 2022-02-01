package org.jboss.fuse.tnb.product.csb;

import org.jboss.fuse.tnb.product.LocalProduct;
import org.jboss.fuse.tnb.product.Product;
import org.jboss.fuse.tnb.product.application.App;
import org.jboss.fuse.tnb.product.csb.application.LocalSpringBootApp;
import org.jboss.fuse.tnb.product.integration.IntegrationBuilder;

import com.google.auto.service.AutoService;

@AutoService(Product.class)
public class LocalCamelSpringBoot extends LocalProduct {
    @Override
    public App createIntegrationApp(IntegrationBuilder integrationBuilder) {
        return new LocalSpringBootApp(integrationBuilder);
    }
}