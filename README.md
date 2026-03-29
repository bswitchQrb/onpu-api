# onpu-api

おんぷクイズのバックエンド API です。

フロントエンド: [onpu](https://github.com/bswitchQrb/onpu)

## システム構成

```
┌─────────────────────────────────────────────────────────┐
│                      Render                             │
│                                                         │
│  ┌─────────────────┐       ┌──────────────────────┐    │
│  │  Static Site     │       │  Web Service (Docker) │    │
│  │  onpu (React)    │──────>│  onpu-api (Spring)    │    │
│  │                  │ HTTPS │                       │    │
│  │  Port: -         │       │  Port: 8080           │    │
│  └─────────────────┘       └──────────┬─────────────┘    │
│                                       │                  │
└───────────────────────────────────────│──────────────────┘
                                        │ JDBC
                              ┌─────────▼──────────┐
                              │  Supabase           │
                              │  PostgreSQL 16      │
                              │  (ap-northeast-1)   │
                              └────────────────────┘
```

## 技術スタック

- **Spring Boot 3.4.4** (Java 17)
- **jOOQ 3.19**（型安全SQL）
- **Flyway**（DBマイグレーション）
- **Spring Security** + **JWT**（認証）
- **PostgreSQL 16**（Supabase）

## API エンドポイント

### 認証（公開）

| メソッド | パス | 説明 |
|---------|------|------|
| POST | `/api/auth/register` | ユーザー登録 |
| POST | `/api/auth/login` | ログイン |

### ユーザー（認証必須）

| メソッド | パス | 説明 |
|---------|------|------|
| GET | `/api/auth/me` | ログインユーザー情報取得 |

### 出題（認証必須）

| メソッド | パス | 説明 |
|---------|------|------|
| POST | `/api/questions/weighted?mode={mode}` | 苦手重み付き出題 |

### 回答・成績（認証必須）

| メソッド | パス | 説明 |
|---------|------|------|
| POST | `/api/answers` | 回答を記録 |
| GET | `/api/answers/history?limit={n}` | 回答履歴取得 |
| GET | `/api/stats` | 全体成績取得 |
| GET | `/api/stats/weak-points?mode={mode}&limit={n}` | 苦手問題取得 |

## DB スキーマ

```sql
users
├── id            BIGSERIAL PRIMARY KEY
├── login_id      VARCHAR(50) NOT NULL UNIQUE
├── password_hash VARCHAR(255) NOT NULL
├── nickname      VARCHAR(50) NOT NULL
├── created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
└── updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP

answer_logs
├── id          BIGSERIAL PRIMARY KEY
├── user_id     BIGINT NOT NULL → users(id)
├── mode        VARCHAR(30) NOT NULL
├── question    VARCHAR(100) NOT NULL
├── is_correct  BOOLEAN NOT NULL
└── answered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    INDEX idx_answer_logs_user_mode (user_id, mode)
```

## ディレクトリ構成

```
src/main/java/com/onpu/
├── OnpuApiApplication.java       # エントリーポイント
├── controller/
│   ├── AuthController.java       # 認証エンドポイント
│   ├── QuestionController.java   # 出題エンドポイント
│   ├── AnswerController.java     # 回答エンドポイント
│   └── GlobalExceptionHandler.java
├── service/
│   ├── AuthService.java          # 認証ロジック
│   ├── QuestionService.java      # 苦手重み付き出題
│   └── AnswerService.java        # 回答記録・成績集計
├── security/
│   ├── SecurityConfig.java       # Spring Security 設定
│   ├── JwtService.java           # JWT生成・検証
│   ├── JwtAuthenticationFilter.java
│   └── JwtProperties.java
├── dto/                          # リクエスト・レスポンス
└── jooq/                         # jOOQ 自動生成コード
```

## 開発

### 前提条件
- Java 17
- Docker（PostgreSQL用）

### ローカル起動

```bash
# PostgreSQL 起動
docker compose up -d

# API 起動
./gradlew bootRun

# jOOQ コード再生成（スキーマ変更時）
./gradlew jooqCodegen
```

API は http://localhost:8080 で起動します。

### 環境変数

| 変数名 | 説明 | デフォルト |
|--------|------|-----------|
| `DATABASE_URL` | JDBC接続URL | `jdbc:postgresql://localhost:5432/onpu` |
| `DATABASE_USERNAME` | DBユーザー名 | `onpu` |
| `DATABASE_PASSWORD` | DBパスワード | `onpu` |
| `JWT_SECRET` | JWT署名キー（256bit以上） | 開発用デフォルト値 |
| `CORS_ALLOWED_ORIGINS` | 許可オリジン（カンマ区切り） | `http://localhost:5173` |
