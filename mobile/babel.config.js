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
      // NOTE: do NOT add react-native-reanimated/plugin manually — modern
      // babel-preset-expo auto-adds the Reanimated/worklets plugin when the
      // package is installed. Adding it here double-registers it and errors.
    ],
  };
};
