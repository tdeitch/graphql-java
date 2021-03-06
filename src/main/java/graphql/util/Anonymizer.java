package graphql.util;

import graphql.Assert;
import graphql.AssertException;
import graphql.analysis.QueryTraverser;
import graphql.analysis.QueryVisitor;
import graphql.analysis.QueryVisitorFieldArgumentEnvironment;
import graphql.analysis.QueryVisitorFieldArgumentInputValue;
import graphql.analysis.QueryVisitorFieldArgumentValueEnvironment;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.analysis.QueryVisitorFragmentSpreadEnvironment;
import graphql.analysis.QueryVisitorInlineFragmentEnvironment;
import graphql.introspection.Introspection;
import graphql.language.Argument;
import graphql.language.AstPrinter;
import graphql.language.AstTransformer;
import graphql.language.Definition;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.IntValue;
import graphql.language.Node;
import graphql.language.NodeVisitorStub;
import graphql.language.OperationDefinition;
import graphql.language.StringValue;
import graphql.language.TypeName;
import graphql.language.VariableDefinition;
import graphql.language.VariableReference;
import graphql.parser.Parser;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNamedSchemaElement;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLSchemaElement;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeVisitorStub;
import graphql.schema.GraphQLUnionType;
import graphql.schema.SchemaTransformer;
import graphql.schema.idl.DirectiveInfo;
import graphql.schema.idl.ScalarInfo;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static graphql.util.TreeTransformerUtil.changeNode;

public class Anonymizer {

    public static class AnonymizeResult {
        private GraphQLSchema schema;
        private List<String> queries;

        public AnonymizeResult(GraphQLSchema schema, List<String> queries) {
            this.schema = schema;
            this.queries = queries;
        }

        public GraphQLSchema getSchema() {
            return schema;
        }

        public List<String> getQueries() {
            return queries;
        }
    }

    public static AnonymizeResult anonymizeSchemaAndQueries(GraphQLSchema schema, List<String> queries) {
        return anonymizeSchemaAndQueries(schema, queries, Collections.emptyMap());
    }

    public static AnonymizeResult anonymizeSchemaAndQueries(GraphQLSchema schema, List<String> queries, Map<String, Object> variables) {

        AtomicInteger objectCounter = new AtomicInteger(1);
        AtomicInteger inputObjectCounter = new AtomicInteger(1);
        AtomicInteger inputObjectFieldCounter = new AtomicInteger(1);
        AtomicInteger fieldCounter = new AtomicInteger(1);
        AtomicInteger scalarCounter = new AtomicInteger(1);
        AtomicInteger directiveCounter = new AtomicInteger(1);
        AtomicInteger argumentCounter = new AtomicInteger(1);
        AtomicInteger interfaceCounter = new AtomicInteger(1);
        AtomicInteger unionCounter = new AtomicInteger(1);
        AtomicInteger enumCounter = new AtomicInteger(1);
        AtomicInteger enumValueCounter = new AtomicInteger(1);
        AtomicInteger defaultStringValueCounter = new AtomicInteger(1);
        AtomicInteger defaultIntValueCounter = new AtomicInteger(1);
        Map<GraphQLNamedSchemaElement, String> newNameMap = new LinkedHashMap<>();

        SchemaTransformer schemaTransformer = new SchemaTransformer();
        GraphQLSchema newSchema = schemaTransformer.transform(schema, new GraphQLTypeVisitorStub() {
            @Override
            public TraversalControl visitGraphQLArgument(GraphQLArgument graphQLArgument, TraverserContext<GraphQLSchemaElement> context) {
                String newName = "argument" + argumentCounter.getAndIncrement();
                newNameMap.put(graphQLArgument, newName);
                GraphQLArgument newElement = graphQLArgument.transform(builder -> {
                    builder.name(newName);
                });
                return changeNode(context, newElement);
            }

            @Override
            public TraversalControl visitGraphQLInterfaceType(GraphQLInterfaceType graphQLInterfaceType, TraverserContext<GraphQLSchemaElement> context) {
                if (Introspection.isIntrospectionTypes(graphQLInterfaceType)) {
                    return TraversalControl.ABORT;
                }
                String newName = "Interface" + interfaceCounter.getAndIncrement();
                newNameMap.put(graphQLInterfaceType, newName);
                GraphQLInterfaceType newElement = graphQLInterfaceType.transform(builder -> {
                    builder.name(newName);
                });
                return changeNode(context, newElement);
            }

            @Override
            public TraversalControl visitGraphQLEnumType(GraphQLEnumType graphQLEnumType, TraverserContext<GraphQLSchemaElement> context) {
                if (Introspection.isIntrospectionTypes(graphQLEnumType)) {
                    return TraversalControl.ABORT;
                }
                String newName = "Enum" + enumCounter.getAndIncrement();
                newNameMap.put(graphQLEnumType, newName);
                GraphQLEnumType newElement = graphQLEnumType.transform(builder -> {
                    builder.name(newName);
                });
                return changeNode(context, newElement);
            }

            @Override
            public TraversalControl visitGraphQLEnumValueDefinition(GraphQLEnumValueDefinition enumValueDefinition, TraverserContext<GraphQLSchemaElement> context) {
                String newName = "EnumValue" + enumValueCounter.getAndIncrement();
                newNameMap.put(enumValueDefinition, newName);
                GraphQLEnumValueDefinition newElement = enumValueDefinition.transform(builder -> {
                    builder.name(newName);
                });
                return changeNode(context, newElement);
            }

            @Override
            public TraversalControl visitGraphQLFieldDefinition(GraphQLFieldDefinition graphQLFieldDefinition, TraverserContext<GraphQLSchemaElement> context) {
                String newName = "field" + fieldCounter.getAndIncrement();
                newNameMap.put(graphQLFieldDefinition, newName);
                GraphQLFieldDefinition newElement = graphQLFieldDefinition.transform(builder -> {
                    builder.name(newName);
                });
                return changeNode(context, newElement);
            }

            @Override
            public TraversalControl visitGraphQLDirective(GraphQLDirective graphQLDirective, TraverserContext<GraphQLSchemaElement> context) {
                if (DirectiveInfo.isGraphqlSpecifiedDirective(graphQLDirective)) {
                    return TraversalControl.ABORT;
                }
                String newName = "Directive" + directiveCounter.getAndIncrement();
                newNameMap.put(graphQLDirective, newName);
                GraphQLDirective newElement = graphQLDirective.transform(builder -> {
                    builder.name(newName);
                });
                return changeNode(context, newElement);
            }

            @Override
            public TraversalControl visitGraphQLInputObjectField(GraphQLInputObjectField graphQLInputObjectField, TraverserContext<GraphQLSchemaElement> context) {
                String newName = "inputField" + inputObjectFieldCounter.getAndIncrement();
                newNameMap.put(graphQLInputObjectField, newName);

                Object defaultValue = graphQLInputObjectField.getDefaultValue();
                if (defaultValue instanceof String) {
                    defaultValue = "defaultValue" + defaultStringValueCounter.getAndIncrement();
                } else if (defaultValue instanceof Integer) {
                    defaultValue = defaultIntValueCounter.getAndIncrement();
                }

                Object finalDefaultValue = defaultValue;
                GraphQLInputObjectField newElement = graphQLInputObjectField.transform(builder -> {
                    builder.name(newName);
                    builder.defaultValue(finalDefaultValue);
                });
                return changeNode(context, newElement);
            }

            @Override
            public TraversalControl visitGraphQLInputObjectType(GraphQLInputObjectType graphQLInputObjectType, TraverserContext<GraphQLSchemaElement> context) {
                if (Introspection.isIntrospectionTypes(graphQLInputObjectType)) {
                    return TraversalControl.ABORT;
                }
                String newName = "InputObject" + inputObjectCounter.getAndIncrement();
                newNameMap.put(graphQLInputObjectType, newName);
                GraphQLInputObjectType newElement = graphQLInputObjectType.transform(builder -> {
                    builder.name(newName);
                });
                return changeNode(context, newElement);
            }


            @Override
            public TraversalControl visitGraphQLObjectType(GraphQLObjectType graphQLObjectType, TraverserContext<GraphQLSchemaElement> context) {
                if (Introspection.isIntrospectionTypes(graphQLObjectType)) {
                    return TraversalControl.ABORT;
                }
                String newName = "Object" + objectCounter.getAndIncrement();
                newNameMap.put(graphQLObjectType, newName);
                GraphQLObjectType newElement = graphQLObjectType.transform(builder -> {
                    builder.name(newName);
                });
                return changeNode(context, newElement);
            }

            @Override
            public TraversalControl visitGraphQLScalarType(GraphQLScalarType graphQLScalarType, TraverserContext<GraphQLSchemaElement> context) {
                if (ScalarInfo.isGraphqlSpecifiedScalar(graphQLScalarType)) {
                    return TraversalControl.ABORT;
                }
                String newName = "Scalar" + scalarCounter.getAndIncrement();
                newNameMap.put(graphQLScalarType, newName);
                GraphQLScalarType newElement = graphQLScalarType.transform(builder -> {
                    builder.name(newName);
                });
                return changeNode(context, newElement);
            }

            @Override
            public TraversalControl visitGraphQLUnionType(GraphQLUnionType graphQLUnionType, TraverserContext<GraphQLSchemaElement> context) {
                if (Introspection.isIntrospectionTypes(graphQLUnionType)) {
                    return TraversalControl.ABORT;
                }
                String newName = "Union" + unionCounter.getAndIncrement();
                newNameMap.put(graphQLUnionType, newName);
                GraphQLUnionType newElement = graphQLUnionType.transform(builder -> {
                    builder.name(newName);
                });
                return changeNode(context, newElement);
            }
        });

        List<String> newQueries = new ArrayList<>();
        for (String query : queries) {
            String newQuery = rewriteQuery(query, schema, newNameMap, variables);
            newQueries.add(newQuery);
        }
        AnonymizeResult result = new AnonymizeResult(newSchema, newQueries);
        return result;
    }

    private static String rewriteQuery(String query, GraphQLSchema schema, Map<GraphQLNamedSchemaElement, String> newNames, Map<String, Object> variables) {
        AtomicInteger fragmentCounter = new AtomicInteger(1);
        AtomicInteger variableCounter = new AtomicInteger(1);
        Map<Node, String> nodeToNewName = new LinkedHashMap<>();
        Map<String, String> variableNames = new LinkedHashMap<>();
        Document document = new Parser().parseDocument(query);
        assertUniqueOperation(document);
        QueryTraverser queryTraverser = QueryTraverser.newQueryTraverser().document(document).schema(schema).variables(variables).build();
        queryTraverser.visitDepthFirst(new QueryVisitor() {

            @Override
            public void visitField(QueryVisitorFieldEnvironment queryVisitorFieldEnvironment) {
                String newName = Assert.assertNotNull(newNames.get(queryVisitorFieldEnvironment.getFieldDefinition()));
                nodeToNewName.put(queryVisitorFieldEnvironment.getField(), newName);
            }

            @Override
            public void visitInlineFragment(QueryVisitorInlineFragmentEnvironment queryVisitorInlineFragmentEnvironment) {
            }

            @Override
            public TraversalControl visitArgumentValue(QueryVisitorFieldArgumentValueEnvironment environment) {
                QueryVisitorFieldArgumentInputValue argumentInputValue = environment.getArgumentInputValue();
                if (argumentInputValue.getValue() instanceof VariableReference) {
                    String name = ((VariableReference) argumentInputValue.getValue()).getName();
                    if (!variableNames.containsKey(name)) {
                        String newName = "var" + variableCounter.getAndIncrement();
                        variableNames.put(name, newName);
                    }
                }
                return TraversalControl.CONTINUE;
            }

            @Override
            public void visitFragmentSpread(QueryVisitorFragmentSpreadEnvironment queryVisitorFragmentSpreadEnvironment) {
                FragmentDefinition fragmentDefinition = queryVisitorFragmentSpreadEnvironment.getFragmentDefinition();
                String newName;
                if (!nodeToNewName.containsKey(fragmentDefinition)) {
                    newName = "Fragment" + fragmentCounter.getAndIncrement();
                    nodeToNewName.put(fragmentDefinition, newName);
                } else {
                    newName = nodeToNewName.get(fragmentDefinition);
                }
                nodeToNewName.put(queryVisitorFragmentSpreadEnvironment.getFragmentSpread(), newName);
            }

            @Override
            public TraversalControl visitArgument(QueryVisitorFieldArgumentEnvironment environment) {
                String newName = Assert.assertNotNull(newNames.get(environment.getGraphQLArgument()));
                nodeToNewName.put(environment.getArgument(), newName);
                return TraversalControl.CONTINUE;
            }
        });

        AtomicInteger stringValueCounter = new AtomicInteger(1);
        AtomicInteger intValueCounter = new AtomicInteger(1);
        AstTransformer astTransformer = new AstTransformer();
        Document newDocument = (Document) astTransformer.transform(document, new NodeVisitorStub() {


            @Override
            public TraversalControl visitStringValue(StringValue node, TraverserContext<Node> context) {
                return changeNode(context, node.transform(builder -> builder.value("stringValue" + stringValueCounter.getAndIncrement())));
            }

            @Override
            public TraversalControl visitIntValue(IntValue node, TraverserContext<Node> context) {
                return changeNode(context, node.transform(builder -> builder.value(BigInteger.valueOf(intValueCounter.getAndIncrement()))));
            }


            @Override
            public TraversalControl visitOperationDefinition(OperationDefinition node, TraverserContext<Node> context) {
                if (node.getName() != null) {
                    return changeNode(context, node.transform(builder -> builder.name("operation")));
                } else {
                    return TraversalControl.CONTINUE;
                }
            }

            @Override
            public TraversalControl visitField(Field node, TraverserContext<Node> context) {
                String newName = Assert.assertNotNull(nodeToNewName.get(node));
                return changeNode(context, node.transform(builder -> builder.name(newName)));
            }

            @Override
            public TraversalControl visitVariableDefinition(VariableDefinition node, TraverserContext<Node> context) {
                String newName = Assert.assertNotNull(variableNames.get(node.getName()));
                return changeNode(context, node.transform(builder -> builder.name(newName)));
            }

            @Override
            public TraversalControl visitVariableReference(VariableReference node, TraverserContext<Node> context) {
                String newName = Assert.assertNotNull(variableNames.get(node.getName()));
                return changeNode(context, node.transform(builder -> builder.name(newName)));
            }

            @Override
            public TraversalControl visitFragmentDefinition(FragmentDefinition node, TraverserContext<Node> context) {
                String newName = Assert.assertNotNull(nodeToNewName.get(node));
                GraphQLType currentCondition = Assert.assertNotNull(schema.getType(node.getTypeCondition().getName()));
                String newCondition = newNames.get(currentCondition);
                return changeNode(context, node.transform(builder -> builder.name(newName).typeCondition(new TypeName(newCondition))));
            }

            @Override
            public TraversalControl visitInlineFragment(InlineFragment node, TraverserContext<Node> context) {
                GraphQLType currentCondition = Assert.assertNotNull(schema.getType(node.getTypeCondition().getName()));
                String newCondition = newNames.get(currentCondition);
                return changeNode(context, node.transform(builder -> builder.typeCondition(new TypeName(newCondition))));
            }

            @Override
            public TraversalControl visitFragmentSpread(FragmentSpread node, TraverserContext<Node> context) {
                String newName = Assert.assertNotNull(nodeToNewName.get(node));
                return changeNode(context, node.transform(builder -> builder.name(newName)));
            }

            @Override
            public TraversalControl visitArgument(Argument node, TraverserContext<Node> context) {
                String newName = Assert.assertNotNull(nodeToNewName.get(node));
                return changeNode(context, node.transform(builder -> builder.name(newName)));
            }
        });
        return AstPrinter.printAstCompact(newDocument);
    }

    private static void assertUniqueOperation(Document document) {
        String operationName = null;
        for (Definition definition : document.getDefinitions()) {
            if (definition instanceof OperationDefinition) {
                if (operationName != null) {
                    throw new AssertException("Query must have exactly one operation");
                }
                OperationDefinition operationDefinition = (OperationDefinition) definition;
                operationName = operationDefinition.getName();
            }
        }
    }

}