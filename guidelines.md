# JitterTicket Development Guidelines

## Project Overview

JitterTicket is an event-sourced concert ticketing application. It allows users to schedule concerts, buy tickets, and manage concert events. The application follows Domain-Driven Design (DDD) principles and uses an event-sourcing architecture to maintain a complete history of all domain events.

## Domain Model

The core domain concepts include:

- **Concert**: A musical event with details like artist, venue, date/time, ticket price, and capacity
- **Customer**: A person who can purchase tickets
- **Ticket**: Represents admission to a concert
  - **Ticket Purchase**: The act of buying tickets
  - **Ticket Order**: A collection of tickets purchased in a single transaction

## Project Structure

The project follows a hexagonal (ports and adapters) architecture:

- `domain`: Contains the core domain model and business logic
  - `concert`: Concert-related domain classes and events
  - `customer`: Customer-related domain classes and events
- `application`: Contains application services that orchestrate domain operations
- `adapter`: Contains adapters for external interfaces
  - `in.web`: Web controllers and view models
  - `out.store`: Persistence-related classes

## Development Workflow

1. **Understand the requirements**: Make sure you understand what you're building before writing code
2. **Write tests first**: Follow TDD principles when implementing new features
3. **Keep the domain model clean**: Avoid adding infrastructure concerns to domain classes
4. **Commit frequently**: Make small, focused commits with clear messages
5. **Run tests before committing**: Always run the "I/O-Free Tests" run configuration before committing changes

## Testing Guidelines

### Test Categories

Tests in this project are categorized using JUnit 5 tags:

- **Domain Tests**: Pure unit tests for domain logic (no tags)
- **Application Tests**: Tests for application services (no tags)
- **Spring Tests**: Tests that require the Spring context (`@Tag("spring")`)
- **MVC Tests**: Tests for web controllers (`@Tag("mvc")`)

### Running Tests

#### I/O-Free Tests

The "I/O-Free Tests" run configuration runs all tests that don't require external I/O operations (database, network, etc.). These tests run quickly and should be run frequently during development.

**IMPORTANT**: Always run the "I/O-Free Tests" run configuration after completing a task to ensure you haven't broken anything.

To run I/O-Free Tests:
1. Select the "I/O-Free Tests" run configuration from the dropdown in the top-right corner of IntelliJ
2. Click the green "Run" button or press Shift+F10

#### All Tests

The "All Tests" run configuration runs all tests, including those that require external dependencies like databases. These tests are slower but provide more comprehensive verification.

Run these tests before submitting a pull request:
1. Select the "All Tests" run configuration
2. Click the green "Run" button or press Shift+F10

## Coding Standards

1. **Follow Java conventions**: Use standard Java naming and coding conventions
2. **Keep methods small**: Methods should do one thing and be easy to understand
3. **Write clear comments**: Especially for complex business logic
4. **Use meaningful names**: Classes, methods, and variables should have descriptive names
5. **Avoid duplication**: Extract common code into reusable methods or classes

## Event Sourcing Guidelines

1. **Events are immutable**: Once created, events should never be modified
2. **Events represent past actions**: Name events in past tense (e.g., `ConcertScheduled`, `TicketsBought`)
3. **Aggregates emit events**: Domain operations on aggregates should emit events
4. **Reconstitute from events**: Aggregates should be able to rebuild their state from events

## Contribution Guidelines

1. **Create a feature branch**: Don't work directly on main
2. **Write tests**: All new features should have tests
3. **Update documentation**: Keep documentation in sync with code changes
4. **Run all tests**: Make sure all tests pass before submitting a PR
5. **Follow code review feedback**: Address all comments from code reviews

## Troubleshooting

If you encounter issues:

1. **Check the logs**: Look for error messages in the application logs
2. **Run specific tests**: Isolate the problem by running specific test classes
3. **Debug step by step**: Use the debugger to step through problematic code
4. **Ask for help**: Don't hesitate to ask teammates for assistance

Remember: Always run the "I/O-Free Tests" run configuration after making changes to ensure you haven't broken anything!