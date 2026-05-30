package config

import (
	"fmt"
	"os"
	"time"

	"github.com/spf13/viper"
)

type Config struct {
	Mode     string         `mapstructure:"mode"`
	Server   ServerConfig   `mapstructure:"server"`
	Database DatabaseConfig `mapstructure:"database"`
	JWT      JWTConfig      `mapstructure:"jwt"`
	Log      LogConfig      `mapstructure:"log"`
}

type ServerConfig struct {
	Port         string `mapstructure:"port"`
	ReadTimeout  int    `mapstructure:"read_timeout"`
	WriteTimeout int    `mapstructure:"write_timeout"`
	IdleTimeout  int    `mapstructure:"idle_timeout"`
}

type DatabaseConfig struct {
	Type            string `mapstructure:"type"`
	Host            string `mapstructure:"host"`
	Port            string `mapstructure:"port"`
	Username        string `mapstructure:"username"`
	Password        string `mapstructure:"password"`
	Database        string `mapstructure:"database"`
	Charset         string `mapstructure:"charset"`
	MaxIdleConns    int    `mapstructure:"max_idle_conns"`
	MaxOpenConns    int    `mapstructure:"max_open_conns"`
	ConnMaxLifetime int    `mapstructure:"conn_max_lifetime"`
	AutoMigrate     bool   `mapstructure:"auto_migrate"`
}

type JWTConfig struct {
	Secret     string        `mapstructure:"secret"`
	ExpireTime time.Duration `mapstructure:"expire_time"`
}

type LogConfig struct {
	Level      string `mapstructure:"level"`
	Format     string `mapstructure:"format"`
	OutputPath string `mapstructure:"output_path"`
}

func Load() *Config {
	// 设置配置文件搜索路径
	viper.SetConfigName("config")
	viper.SetConfigType("yaml")
	viper.AddConfigPath("./configs")
	viper.AddConfigPath(".")

	// 读取配置文件
	if err := viper.ReadInConfig(); err != nil {
		fmt.Printf("Warning: config file not found, using defaults: %v\n", err)
	}

	// 环境变量覆盖
	viper.AutomaticEnv()
	viper.SetEnvPrefix("CRM")

	var config Config
	if err := viper.Unmarshal(&config); err != nil {
		fmt.Printf("Failed to unmarshal config: %v\n", err)
		os.Exit(1)
	}

	// 设置默认值
	setDefaults(&config)

	return &config
}

func setDefaults(cfg *Config) {
	if cfg.Mode == "" {
		cfg.Mode = "debug"
	}

	if cfg.Server.Port == "" {
		cfg.Server.Port = "8080"
	}

	if cfg.Server.ReadTimeout == 0 {
		cfg.Server.ReadTimeout = 30
	}

	if cfg.Server.WriteTimeout == 0 {
		cfg.Server.WriteTimeout = 30
	}

	if cfg.Server.IdleTimeout == 0 {
		cfg.Server.IdleTimeout = 120
	}

	if cfg.Database.Type == "" {
		cfg.Database.Type = "mysql"
	}

	if cfg.Database.Charset == "" {
		cfg.Database.Charset = "utf8mb4"
	}

	if cfg.Database.MaxIdleConns == 0 {
		cfg.Database.MaxIdleConns = 10
	}

	if cfg.Database.MaxOpenConns == 0 {
		cfg.Database.MaxOpenConns = 100
	}

	if cfg.Database.ConnMaxLifetime == 0 {
		cfg.Database.ConnMaxLifetime = 3600
	}

	if cfg.JWT.ExpireTime == 0 {
		cfg.JWT.ExpireTime = 24 * time.Hour
	}

	if cfg.Log.Level == "" {
		cfg.Log.Level = "info"
	}

	if cfg.Log.Format == "" {
		cfg.Log.Format = "json"
	}
}
