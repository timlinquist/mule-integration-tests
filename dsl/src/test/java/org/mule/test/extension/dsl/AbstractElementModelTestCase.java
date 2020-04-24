/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.extension.dsl;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.config.internal.dsl.xml.XmlNamespaceInfoProviderSupplier.createFromExtensionModels;
import static org.mule.runtime.dsl.api.xml.parser.XmlConfigurationProcessor.processXmlConfiguration;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.util.ResourceLocator;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.ElementDeclaration;
import org.mule.runtime.config.api.dsl.model.DslElementModel;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.config.api.dsl.processor.ArtifactConfig;
import org.mule.runtime.config.internal.ModuleDelegatingEntityResolver;
import org.mule.runtime.config.internal.model.ApplicationModel;
import org.mule.runtime.core.api.extension.MuleExtensionModelProvider;
import org.mule.runtime.core.api.util.xmlsecurity.XMLSecureFactories;
import org.mule.runtime.core.internal.util.DefaultResourceLocator;
import org.mule.runtime.dsl.api.ConfigResource;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;
import org.mule.runtime.dsl.api.xml.parser.ConfigFile;
import org.mule.runtime.dsl.api.xml.parser.ParsingPropertyResolver;
import org.mule.runtime.dsl.api.xml.parser.XmlConfigurationDocumentLoader;
import org.mule.runtime.dsl.api.xml.parser.XmlParsingConfiguration;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.Before;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;

import com.google.common.collect.ImmutableSet;

@ArtifactClassLoaderRunnerConfig(applicationSharedRuntimeLibs = {
    "org.apache.derby:derby",
    "org.apache.activemq:activemq-client",
    "org.apache.activemq:activemq-broker",
    "org.apache.activemq:activemq-kahadb-store"})
public abstract class AbstractElementModelTestCase extends MuleArtifactFunctionalTestCase {

  protected static final String DB_CONFIG = "dbConfig";
  protected static final String DB_NS = "db";
  protected static final String HTTP_LISTENER_CONFIG = "httpListener";
  protected static final String HTTP_REQUESTER_CONFIG = "httpRequester";
  protected static final String HTTP_NS = "http";
  protected static final String COMPONENTS_FLOW = "testFlow";
  protected static final int LISTENER_PATH = 0;
  protected static final int DB_BULK_INSERT_PATH = 2;
  protected static final int REQUESTER_PATH = 3;
  protected static final int DB_INSERT_PATH = 4;

  protected DslResolvingContext dslContext;
  protected DslElementModelFactory modelResolver;
  protected ApplicationModel applicationModel;
  protected Document doc;

  @Before
  public void setup() throws Exception {
    Set<ExtensionModel> extensions = muleContext.getExtensionManager().getExtensions();
    dslContext = DslResolvingContext.getDefault(ImmutableSet.<ExtensionModel>builder()
        .addAll(extensions)
        .add(MuleExtensionModelProvider.getExtensionModel()).build());
    modelResolver = DslElementModelFactory.getDefault(dslContext);
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
  }

  // Scaffolding
  protected <T extends NamedObject> DslElementModel<T> resolve(ComponentConfiguration component) {
    Optional<DslElementModel<T>> elementModel = modelResolver.create(component);
    assertThat(elementModel.isPresent(), is(true));
    return elementModel.get();
  }

  protected <T extends NamedObject> DslElementModel<T> resolve(ElementDeclaration component) {
    Optional<DslElementModel<T>> elementModel = modelResolver.create(component);
    assertThat(elementModel.isPresent(), is(true));
    return elementModel.get();
  }

  protected ComponentConfiguration getAppElement(ApplicationModel applicationModel, String name) {
    Optional<ComponentConfiguration> component =
        applicationModel.findTopLevelNamedComponent(name).map(componentModel -> componentModel.getConfiguration());
    assertThat(component.isPresent(), is(true));
    return component.get();
  }

  protected <T> DslElementModel<T> getChild(DslElementModel<? extends NamedObject> parent, ComponentConfiguration component) {
    return getChild(parent, component.getIdentifier());
  }

  protected <T> DslElementModel<T> getChild(DslElementModel<? extends NamedObject> parent,
                                            ComponentIdentifier identifier) {
    Optional<DslElementModel<T>> elementModel = parent.findElement(identifier);
    assertThat(format("Failed fetching child '%s' from parent '%s'", identifier.getName(),
                      parent.getModel().getName()),
               elementModel.isPresent(), is(true));
    return elementModel.get();
  }

  protected <T> DslElementModel<T> getChild(DslElementModel<? extends NamedObject> parent,
                                            String name) {
    Optional<DslElementModel<T>> elementModel = parent.findElement(name);
    assertThat(format("Failed fetching child '%s' from parent '%s'", name,
                      parent.getModel().getName()),
               elementModel.isPresent(), is(true));
    return elementModel.get();
  }

  private <T> DslElementModel<T> getAttribute(DslElementModel<? extends NamedObject> parent, String component) {
    Optional<DslElementModel<T>> elementModel = parent.findElement(component);
    assertThat(format("Failed fetching attribute '%s' from parent '%s'", component, parent.getModel().getName()),
               elementModel.isPresent(), is(true));
    return elementModel.get();
  }

  protected ComponentIdentifier newIdentifier(String name, String ns) {
    return builder().name(name).namespace(ns).build();
  }

  protected void assertHasParameter(ParameterizedModel model, String name) {
    assertThat(model.getAllParameterModels()
        .stream().anyMatch(p -> p.getName().equals(name)), is(true));
  }

  protected void assertAttributeIsPresent(DslElementModel<? extends ParameterizedModel> element, String name) {
    assertHasParameter(element.getModel(), name);
    DslElementModel<NamedObject> databaseParam = getAttribute(element, name);
    assertThat(databaseParam.getDsl().supportsAttributeDeclaration(), is(true));
    assertThat(databaseParam.getDsl().supportsChildDeclaration(), is(false));
  }

  protected void assertElementName(DslElementModel propertiesElement, String name) {
    assertThat(propertiesElement.getDsl().getElementName(), is(name));
  }

  // Scaffolding
  protected ApplicationModel loadApplicationModel() throws Exception {
    return loadApplicationModel(getConfigFile());
  }

  // TODO MULE-17199 (AST) use an AST parser api
  protected ApplicationModel loadApplicationModel(String configFile) throws Exception {
    InputStream appIs = Thread.currentThread().getContextClassLoader().getResourceAsStream(configFile);
    checkArgument(appIs != null, "The given application was not found as resource");

    List<ConfigFile> configFiles = processXmlConfiguration(new XmlParsingConfiguration() {

      @Override
      public ParsingPropertyResolver getParsingPropertyResolver() {
        return key -> key;
      }

      @Override
      public ConfigResource[] getArtifactConfigResources() {
        return new ConfigResource[] {new ConfigResource(configFile, appIs)};
      }

      @Override
      public ResourceLocator getResourceLocator() {
        return new DefaultResourceLocator();
      }

      @Override
      public Supplier<SAXParserFactory> getSaxParserFactory() {
        return () -> XMLSecureFactories.createDefault().getSAXParserFactory();
      }

      @Override
      public XmlConfigurationDocumentLoader getXmlConfigurationDocumentLoader() {
        return XmlConfigurationDocumentLoader.schemaValidatingDocumentLoader();
      }

      @Override
      public EntityResolver getEntityResolver() {
        return new ModuleDelegatingEntityResolver(muleContext.getExtensionManager().getExtensions());
      }

      @Override
      public List<XmlNamespaceInfoProvider> getXmlNamespaceInfoProvider() {
        return createFromExtensionModels(muleContext.getExtensionManager().getExtensions(), Optional.empty());
      }
    });

    ArtifactConfig artifactConfig = new ArtifactConfig.Builder()
        .addConfigFile(configFiles.get(0))
        .build();

    return new ApplicationModel(artifactConfig, new ArtifactDeclaration(),
                                uri -> muleContext.getExecutionClassLoader().getResourceAsStream(uri));
  }

  protected String write() throws Exception {
    // write the content into xml file
    TransformerFactory transformerFactory = TransformerFactory.newInstance();

    Transformer transformer = transformerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

    DOMSource source = new DOMSource(doc);
    StringWriter writer = new StringWriter();
    transformer.transform(source, new StreamResult(writer));
    return writer.getBuffer().toString().replaceAll("\n|\r", "");
  }

  protected void createAppDocument() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder docBuilder = factory.newDocumentBuilder();

    this.doc = docBuilder.newDocument();
    Element mule = doc.createElement("mule");
    doc.appendChild(mule);
    mule.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", "http://www.mulesoft.org/schema/mule/core");
    mule.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance",
                        "xsi:schemaLocation", getExpectedSchemaLocation());
  }

  protected String getExpectedSchemaLocation() {
    return "http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd";
  }

  protected void assertValue(DslElementModel elementModel, String value) {
    assertThat(elementModel.getValue().get(), is(value));
  }

}
