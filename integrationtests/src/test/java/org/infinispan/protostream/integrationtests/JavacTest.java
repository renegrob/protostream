package org.infinispan.protostream.integrationtests;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static javax.tools.StandardLocation.CLASS_OUTPUT;
import static javax.tools.StandardLocation.SOURCE_OUTPUT;
import static org.junit.Assert.assertTrue;

import org.infinispan.protostream.annotations.impl.processor.AutoProtoSchemaBuilderAnnotationProcessor;
import org.junit.Test;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;

/**
 * @author anistor@redhat.com
 * @since 4.3
 */
public class JavacTest {

   private static final String src1 = "package test;\n" +
         "import org.infinispan.protostream.annotations.ProtoField;\n" +
         "public class TestMessage {\n" +
         "   @ProtoField(number = 1, required = true)\n" +
         "   boolean flag;\n" +
         "}\n";

   private static final String src2 = "package test;\n" +
         "import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;\n" +
         "import org.infinispan.protostream.SerializationContextInitializer;\n" +
         "@AutoProtoSchemaBuilder(schemaFilePath = \"second_initializer\", className = \"TestInitializer\",\n" +
         "         packages = {\"org.infinispan.protostream.integrationtests\", \"test\"}, service = true)\n" +
         "public abstract class SecondInitializer implements SerializationContextInitializer {\n" +
         "}\n";

   private static final String src3 = "package test_depends;\n" +
         "import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;\n" +
         "import org.infinispan.protostream.annotations.ProtoField;\n" +
         "import org.infinispan.protostream.SerializationContextInitializer;\n" +
         "import test.TestMessage;\n" +
         "@AutoProtoSchemaBuilder(schemaFilePath = \"/\", dependsOn = test.SecondInitializer.class,\n" +
         "         classes = DependentInitializer.A.class, autoImportClasses = false, service = true)\n" +
         "interface DependentInitializer extends SerializationContextInitializer {\n" +
         "   class A {\n" +
         "      @ProtoField(number = 1, required = true)\n" +
         "      public TestMessage testMessage;\n" +
         "   }\n\n" +
         "   default String getProtoFileName() {\n" +
         "      return null;\n" +
         "   }" +
         "}\n";

   @Test
   public void testAnnotationProcessing() {
      Compilation compilation =
            javac().withProcessors(new AutoProtoSchemaBuilderAnnotationProcessor())
                  .compile(JavaFileObjects.forSourceString("TestMessage", src1),
                        JavaFileObjects.forSourceString("SecondInitializer", src2),
                        JavaFileObjects.forSourceString("DependentInitializer", src3));

      assertThat(compilation).succeeded();
      assertTrue(compilation.generatedFile(SOURCE_OUTPUT, "test/TestInitializer.java").isPresent());
      assertTrue(compilation.generatedFile(SOURCE_OUTPUT, "test_depends/DependentInitializerImpl.java").isPresent());
      assertTrue(compilation.generatedFile(CLASS_OUTPUT, "second_initializer/SecondInitializer.proto").isPresent());
      assertTrue(compilation.generatedFile(CLASS_OUTPUT, "DependentInitializer.proto").isPresent());
      assertTrue(compilation.generatedFile(CLASS_OUTPUT, "META-INF/services/org.infinispan.protostream.SerializationContextInitializer").isPresent());
   }
}