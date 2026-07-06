module.exports = function (api) {
  api.cache(true);
  return {
    presets: ["babel-preset-expo"],
    plugins: [
      // Path alias @/* -> ./src/*
      [
        "module-resolver",
        {
          root: ["."],
          alias: { "@": "./src" },
          extensions: [".ts", ".tsx", ".js", ".jsx", ".json"],
        },
      ],
      // react-native-reanimated plugin must be listed LAST.
      "react-native-reanimated/plugin",
    ],
  };
};
