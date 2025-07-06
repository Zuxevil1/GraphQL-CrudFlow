package com.example;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.*;

import java.io.InputStream;
import java.io.InputStreamReader;

public class GraphQLConfig {

    public static GraphQL init() throws Exception {
        InputStream schemaStream = GraphQLConfig.class.getResourceAsStream("/schema.graphqls");
        if (schemaStream == null) {
            throw new RuntimeException("schema.graphqls not found in resources folder");
        }

        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(new InputStreamReader(schemaStream));

        RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
            .type("Query", builder -> builder
                .dataFetcher("allProducts", env -> ProductRepository.findAll())
            )
            .type("Mutation", builder -> builder
                .dataFetcher("addProduct", env -> {
                    String name = env.getArgument("name");
                    Double price = ((Number) env.getArgument("price")).doubleValue();
                    String category = env.getArgument("category");
                    return ProductRepository.add(name, price, category);
                })
                .dataFetcher("updateProduct", env -> {
                    Long id = Long.parseLong(env.getArgument("id"));
                    Product p = ProductRepository.findById(id);
                    if (p == null) return null;
                    p.name = env.getArgument("name");
                    p.price = ((Number) env.getArgument("price")).doubleValue();
                    p.category = env.getArgument("category");
                    return p;
                })
                .dataFetcher("deleteProduct", env -> {
                    Long id = Long.parseLong(env.getArgument("id"));
                    Product p = ProductRepository.findById(id);
                    ProductRepository.delete(id);
                    return p;
                })
            )
            .build();

        SchemaGenerator generator = new SchemaGenerator();
        GraphQLSchema schema = generator.makeExecutableSchema(typeRegistry, wiring);
        return GraphQL.newGraphQL(schema).build();
    }
}
