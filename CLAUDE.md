# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands
- Build project: `mvn clean install`
- Run tests: `mvn test`
- Run single test: `mvn test -Dtest=TestClassName#testMethodName`
- Generate Javadoc: `mvn javadoc:javadoc`
- Run test coverage analysis: `mvn verify` (JaCoCo report in target/site/jacoco)

## Code Style Guidelines
- **Java Version**: JDK 17
- **Indentation**: 4 spaces, no tabs
- **Naming**: PascalCase for classes, camelCase for methods/variables, ALL_CAPS for constants, interfaces prefixed with 'I'
- **Imports**: Organized by package, no wildcards, alphabetical within groups
- **Error Handling**: Custom exceptions extending RuntimeException (WhatsappApiException), detailed error objects
- **Type Safety**: Use enums for constants, generics for collections, immutable objects with final fields
- **Patterns**: Builder pattern for complex objects, Factory pattern for creation, APIs using Retrofit
- **Documentation**: Comprehensive Javadoc with @param, @return, @see tags
- **Testing**: JUnit 5 with Mockito, use MockWebServer for API tests