# llm-eval Component: Database Layer (Phase 1)

## Completed Files

### 1. Component Configuration
- `deps.edn` - Component dependencies including Datalevin, Clojure, ClojureScript

### 2. Database Layer (db.clj)
**Schema:**
- `evaluation` entity:
  - `:evaluation/id` (string, unique identity)
  - `:evaluation/input` (string)
  - `:evaluation/output` (string)
  - `:evaluation/model-name` (string)
  - `:evaluation/timestamp` (instant)
  - `:evaluation/prompt-metadata` (string - JSON)
  - `:evaluation/scores` (many-to-one reference to evaluation-score)

- `evaluation-score` entity:
  - `:evaluation-score/id` (string, unique identity)
  - `:evaluation-score/criterion-name` (string)
  - `:evaluation-score/score-value` (float)
  - `:evaluation-score/judge-type` (string - "human" or "llm")
  - `:evaluation-score/feedback` (string)
  - `:evaluation-score/evaluation` (reference to evaluation)

**Functions:**
- `db-empty?` - Check if database is empty
- `create-evaluation!` - Insert evaluation record
- `create-score!` - Insert score record
- `get-evaluation-by-id` - Retrieve single evaluation with scores
- `get-evaluations` - Query with filters, pagination, sorting
- `get-evaluation-stats` - Get aggregate statistics
- `get-score-distributions` - Get score distribution for charts
- `get-model-names` - Get distinct model names
- `get-criteria-names` - Get distinct criterion names
- `seed-data!` - Create 50 sample evaluations with scores

### 3. API Layer (api.clj)
**Query Handlers:**
- `:query/evaluations` - Paginated evaluations with filters/sort
- `:query/evaluation-by-id` - Single evaluation by ID
- `:query/evaluation-stats` - Dashboard statistics
- `:query/score-distributions` - Chart data
- `:query/model-names` - Available model names
- `:query/criteria-names` - Available criteria

**Command Handlers:**
- `:command/create-evaluation` - Create new evaluation
- `:command/create-score` - Create new score

### 4. Public Interface (interface.clj)
Re-exports all database and API functions for external components.

### 5. Tests
- `db_test.clj` - Tests for all database operations
- `api_test.clj` - Tests for query/command handlers

## Data Structure

### Evaluation Entity
```clojure
{:evaluation/id "eval-0001"
 :evaluation/input "What is the capital of France?"
 :evaluation/output "The capital of France is Paris."
 :evaluation/model-name "gpt-4"
 :evaluation/timestamp #inst "2024-01-30T..."
 :evaluation/prompt-metadata "{\"template\":\"standard_prompt\",...}"
 :evaluation/scores [...] ;; List of evaluation-score entities
}
```

### Evaluation Score Entity
```clojure
{:evaluation-score/id "score-eval-0001-accuracy-human"
 :evaluation-score/criterion-name "accuracy"
 :evaluation-score/score-value 8.0
 :evaluation-score/judge-type "human"
 :evaluation-score/feedback "Human feedback for accuracy"
 :evaluation-score/evaluation [:evaluation/id "eval-0001"] ;; Reference
}
```

### Query Response Format
```clojure
{:data [...evaluation-entities...]
 :total 500
 :page 1
 :size 50
 :total-pages 10}
```

### Statistics Format
```clojure
{:total-evaluations 500
 :model-counts {"gpt-4" 250 "claude-3-opus" 150 ...}
 :avg-scores {"accuracy" 8.5 "helpfulness" 7.8 ...}
 :date-range {:earliest #inst "..." :latest #inst "..."}}
```

## Seed Data

The `seed-data!` function creates 50 sample evaluations:
- 4 models: gpt-4, claude-3-opus, claude-3-sonnet, gemini-pro
- 4 criteria: accuracy, helpfulness, clarity, safety
- 2 scores per criterion: human and LLM judge
- Timestamps spread over 50 hours
- Sample inputs/outputs

## Usage

### Loading and Seeding Database
```clojure
(require '[com.zihao.llm-eval.db :as db])
(db/seed-data!)  ;; Creates 50 sample evaluations
```

### Querying Evaluations
```clojure
(db/get-evaluations {:model-names ["gpt-4"]
                    :score-min 5.0
                    :judge-type "human"} 
                   1 50 :timestamp :desc)
```

### Getting Statistics
```clojure
(db/get-evaluation-stats)
(db/get-score-distributions)
```

### Using API Handlers
```clojure
(api/query-handler {:command/kind :query/evaluations
                   :query-id 1
                   :command/data {...}})
```

## Next Steps (Phase 2)

1. Create `llm-auth` component for role-based authentication
2. Implement frontend view-functions (dashboard, dataset, detail)
3. Implement actions for frontend interactions
4. Implement router with routes
5. Integrate with web-app base

## Notes

- Database stored at `databases/llm-eval.db`
- Datalevin used for persistence (consistent with login component)
- Filtering, pagination, and sorting implemented in Clojure
- Seed data generation follows original Next.js pattern
- API handlers follow project's query/command pattern
