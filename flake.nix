{
  description = "Objectifier Client";

  inputs = {
    nixpkgs.url = "nixpkgs/nixos-22.11";
    helpers = {
      url = "git+https://git.fudo.org/fudo-public/nix-helpers.git";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, helpers, utils, ... }:
    utils.lib.eachDefaultSystem (system: {
      packages = rec {
        default = objectifier-client;
        objectifier-client = helpers.packages."${system}".mkClojureBin {
          name = "org.fudo/objectifier-client";
          primary-namespace = "objectifier-client.cli";
          src = ./.;
        };
      };

      devShells = let pkgs = import nixpkgs { inherit system; };
      in rec {
        default = update-deps;
        objectifier-client = pkgs.mkShell {
          buildInputs = [ self.packages."${system}".objectifier-client ];
        };
        update-deps = pkgs.mkShell {
          buildInputs = with helpers.packages."${system}";
            [ updateClojureDeps ];
        };
      };
    });
}
