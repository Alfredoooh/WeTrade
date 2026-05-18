const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');

module.exports = {
  entry: path.resolve(__dirname, 'index.js'),
  output: {
    filename: 'bundle.[contenthash].js',
    path: path.resolve(__dirname, 'dist'),
    publicPath: '/',
    clean: true,
  },
  resolve: {
    alias: { 'react-native$': 'react-native-web' },
    extensions: ['.web.js', '.js', '.web.jsx', '.jsx'],
  },
  module: {
    rules: [{
      test: /\.(js|jsx)$/,
      include: [
        path.resolve(__dirname, 'index.js'),
        path.resolve(__dirname, 'src'),
        path.resolve(__dirname, 'node_modules/react-native-web'),
      ],
      use: { loader: 'babel-loader', options: { cacheDirectory: true } },
    }],
  },
  plugins: [
    new HtmlWebpackPlugin({
      template: path.resolve(__dirname, 'public/index.html'),
    }),
  ],
  devServer: {
    port: 3000,
    historyApiFallback: true,
    hot: true,
  },
};